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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package uk.ac.ed.jpai.samples.images;

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
import uk.ac.ed.jpai.ArrayFunction;
import uk.ac.ed.jpai.MapAccelerator;

public class BWConversion {

    public static class LoadImage extends Component {

        private static final long serialVersionUID = 1L;
        private BufferedImage image;

        public LoadImage() {
            try {
                image = ImageIO.read(new File("/tmp/image.png"));
            } catch (IOException e) {
            }
        }

        @SuppressWarnings("unused")
        private void sequentialComputation() {

            int w = this.image.getWidth();
            int s = this.image.getHeight();

            for (int i = 0; i < w; i++) {
                for (int j = 0; j < s; j++) {

                    int rgb = image.getRGB(i, j);

                    int alpha = (rgb >> 24) & 0xff;
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = (rgb & 0xFF);

                    int grayLevel = (red + green + blue) / 3;
                    int gray = (alpha << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;

                    image.setRGB(i, j, gray);
                }
            }

        }

        @Override
        public void paint(Graphics g) {

            int w = this.image.getWidth();
            int s = this.image.getHeight();

            // Transform the input data into PArray Form
            PArray<Integer> color = new PArray<>(w * s, TypeFactory.Integer());
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < s; j++) {
                    int rgb = image.getRGB(i, j);
                    color.put(i * s + j, rgb);
                }
            }

            // Kernel transformation from RGB to Gray scale
            ArrayFunction<Integer, Integer> function = new MapAccelerator<>(rgb -> {
                int alpha = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb & 0xFF);

                int grayLevel = (red + green + blue) / 3;
                int gray = (alpha << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;
                return gray;
            });

            // Execute the kernel on the GPU
            PArray<Integer> result = function.apply(color);

            // Set the final result to the final image
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < s; j++) {
                    image.setRGB(i, j, result.get((i) * s + j));
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
        JFrame frame = new JFrame("Image Grey-scale conversion example with JPAI and Marawacc");

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
