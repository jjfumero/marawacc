/*
 * Copyright (c) 2013, 2017, The University of Edinburgh. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package uk.ac.ed.jpai.samples.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import uk.ac.ed.datastructures.common.PArray;
import uk.ac.ed.datastructures.common.TypeFactory;
import uk.ac.ed.datastructures.tuples.Tuple2;
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;

/**
 * It applies a Blur filter to an input image. Algorithm taken from CUDA course CS344 in Udacity.
 *
 */
public class BlurTransformation {

    @SuppressWarnings("serial")
    public static class LoadImage extends Component {

        private BufferedImage image;
        private static boolean printed = false;

        private static final boolean PARALLEL_COMPUTATION = true;

        public LoadImage() {
            try {
                image = ImageIO.read(new File("/tmp/image.png"));
            } catch (IOException e) {
                System.out.println("File not found");
                System.exit(0);
            }
        }

        private static void channelConvolutionSequential(int[] channel, int[] channelBlurred, final int numRows, final int numCols, float[] filter, final int filterWidth) {
            // Dealing with an even width filter is trickier
            assert (filterWidth % 2 == 1);

            // For every pixel in the image
            for (int r = 0; r < numRows; ++r) {
                for (int c = 0; c < numCols; ++c) {
                    float result = 0.f;
                    // For every value in the filter around the pixel (c, r)
                    for (int filter_r = -filterWidth / 2; filter_r <= filterWidth / 2; ++filter_r) {
                        for (int filter_c = -filterWidth / 2; filter_c <= filterWidth / 2; ++filter_c) {
                            // Find the global image position for this filter position
                            // clamp to boundary of the image
                            int image_r = Math.min(Math.max(r + filter_r, 0), (numRows - 1));
                            int image_c = Math.min(Math.max(c + filter_c, 0), (numCols - 1));

                            float image_value = (channel[image_r * numCols + image_c]);
                            float filter_value = filter[(filter_r + filterWidth / 2) * filterWidth + filter_c + filterWidth / 2];

                            result += image_value * filter_value;
                        }
                    }
                    channelBlurred[r * numCols + c] = result > 255 ? 255 : (int) result;
                }
            }
        }

        private static PArray<Integer> channelConvolutionWithJPAI(int[] channel, final int numRows, final int numCols, float[] filter, final int filterWidth) {
            // Dealing with an even width filter is trickier
            assert (filterWidth % 2 == 1);

            PArray<Tuple2<Integer, Integer>> input = new PArray<>(numRows * numCols, TypeFactory.Tuple("Tuple2<Integer, Integer>"));
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    input.put(i * numCols + j, new Tuple2<>(i, j));
                }
            }

            ArrayFunction<Tuple2<Integer, Integer>, Integer> blurFunction = new MapAccelerator<>(t -> {

                int r = t._1();
                int c = t._2();
                float result = 0.f;
                for (int filter_r = -filterWidth / 2; filter_r <= filterWidth / 2; ++filter_r) {
                    for (int filter_c = -filterWidth / 2; filter_c <= filterWidth / 2; ++filter_c) {
                        // Find the global image position for this filter position
                        // clamp to boundary of the image
                        int image_r = Math.min(Math.max(r + filter_r, 0), (numRows - 1));
                        int image_c = Math.min(Math.max(c + filter_c, 0), (numCols - 1));

                        float image_value = (channel[image_r * numCols + image_c]);
                        float filter_value = filter[(filter_r + filterWidth / 2) * filterWidth + filter_c + filterWidth / 2];

                        result += image_value * filter_value;
                    }
                }
                return result > 255 ? 255 : (int) result;
            });

            PArray<Integer> output = blurFunction.apply(input);
            return output;
        }

        private void sequentialComputation() {

            int w = image.getWidth();
            int h = image.getHeight();

            int[] redChannel = new int[w * h];
            int[] greenChannel = new int[w * h];
            int[] blueChannel = new int[w * h];
            int[] alphaChannel = new int[w * h];

            int[] redFilter = new int[w * h];
            int[] greenFilter = new int[w * h];
            int[] blueFilter = new int[w * h];

            float[] filter = new float[w * h];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    filter[i * h + j] = 1.f / 49.f;
                }
            }

            // data initialisation
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int rgb = image.getRGB(i, j);
                    alphaChannel[i * h + j] = (rgb >> 24) & 0xFF;
                    redChannel[i * h + j] = (rgb >> 16) & 0xFF;
                    greenChannel[i * h + j] = (rgb >> 8) & 0xFF;
                    blueChannel[i * h + j] = (rgb & 0xFF);
                }
            }

            int filterWidth = 7;

            long start = System.nanoTime();
            channelConvolutionSequential(redChannel, redFilter, w, h, filter, filterWidth);
            channelConvolutionSequential(greenChannel, greenFilter, w, h, filter, filterWidth);
            channelConvolutionSequential(blueChannel, blueFilter, w, h, filter, filterWidth);

            // now recombine into the output image - Alpha is 255 for no transparency
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    Color c = new Color(redFilter[i * h + j], greenFilter[i * h + j], blueFilter[i * h + j], alphaChannel[i * h + j]);
                    image.setRGB(i, j, c.getRGB());
                }
            }
            long end = System.nanoTime();
            System.out.println("Total time: " + (end - start) + " (ns)");

            printed = true;
        }

        private void parallelComputation() {

            int w = image.getWidth();
            int h = image.getHeight();

            int[] redChannel = new int[w * h];
            int[] greenChannel = new int[w * h];
            int[] blueChannel = new int[w * h];
            int[] alphaChannel = new int[w * h];

            float[] filter = new float[w * h];
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    filter[i * h + j] = 1.f / 49.f;
                }
            }

            // data initialisation
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int rgb = image.getRGB(i, j);
                    alphaChannel[i * h + j] = (rgb >> 24) & 0xFF;
                    redChannel[i * h + j] = (rgb >> 16) & 0xFF;
                    greenChannel[i * h + j] = (rgb >> 8) & 0xFF;
                    blueChannel[i * h + j] = (rgb & 0xFF);
                }
            }

            int filterWidth = 7;

            long start = System.nanoTime();
            PArray<Integer> resultRed = channelConvolutionWithJPAI(redChannel, w, h, filter, filterWidth);
            PArray<Integer> resultGreen = channelConvolutionWithJPAI(greenChannel, w, h, filter, filterWidth);
            PArray<Integer> resultBlue = channelConvolutionWithJPAI(blueChannel, w, h, filter, filterWidth);

            // now recombine into the output image - Alpha is 255 for no transparency
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    Color c = new Color(resultRed.get(i * h + j), resultBlue.get(i * h + j), resultBlue.get(i * h + j), alphaChannel[i * h + j]);
                    image.setRGB(i, j, c.getRGB());
                }
            }
            long end = System.nanoTime();
            System.out.println("Total time: " + (end - start) + " (ns)");

            printed = true;
        }

        @Override
        public void paint(Graphics g) {
            if (!printed) {
                if (PARALLEL_COMPUTATION) {
                    parallelComputation();
                } else {
                    sequentialComputation();
                }
            }
            // draw the image
            g.drawImage(this.image, 0, 0, null);
        }

        @Override
        public Dimension getPreferredSize() {
            if (image == null) {
                return new Dimension(100, 100);
            } else {
                return new Dimension(image.getWidth(), image.getHeight());
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Blur Filter Example with JPAI and Marawacc");

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });

        frame.add(new LoadImage());
        frame.pack();
        frame.setVisible(true);
    }

}
