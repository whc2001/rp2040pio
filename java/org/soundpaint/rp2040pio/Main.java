/*
 * @(#)Main.java 1.00 21/01/31
 *
 * Copyright (C) 2021 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For updates and more info or contacting the author, visit:
 * <https://github.com/soundpaint/rp2040pio>
 *
 * Author's web site: www.juergen-reuter.de
 */
package org.soundpaint.rp2040pio;

import java.io.IOException;

public class Main
{
  private final PIO pio;
  private final GPIO gpio;

  public Main()
  {
    pio = new PIO(0, MasterClock.getDefaultInstance()); //PIO.PIO0;
    gpio = pio.getGPIO();
  }

  public void run() throws IOException
  {
    final String programResourcePath = "/examples/squarewave.hex";
    //final String programResourcePath = "/examples/ws2812.hex";
    //final Monitor monitor = new Monitor();
    //monitor.addProgram(programResourcePath);
    //monitor.setSideSetCount(1);
    //monitor.dumpProgram();
    final TimingDiagram diagram = new TimingDiagram(pio);
    diagram.addProgram(programResourcePath);
    diagram.addSignal(DiagramConfig.createClockSignal("clock"));
    diagram.addSignal(new
                      DiagramConfig.BitSignal("SM0_CLK_ENABLE",
                                              () ->
                                              Bit.fromValue(pio.getSM(0).
                                                            getPLL().
                                                            getClockEnable())));
    diagram.addSignal(DiagramConfig.createGPIOBitSignal(null, gpio, 0));
    diagram.addSignal(DiagramConfig.createGPIOValueSignal(null, gpio, 0));
    diagram.addSignal(DiagramConfig.createGPIOValueSignal(null, gpio, 1));
    diagram.addSignal(DiagramConfig.createGPIOValueSignal(null, gpio, 10));
    diagram.addSignal(DiagramConfig.createPCStateSignal(null, pio, 0, false));
    diagram.addSignal(DiagramConfig.createPCStateSignal(null, pio, 0, true));
    diagram.addSignal(DiagramConfig.createInstructionSignal(null, pio, 0, false, true));
    diagram.addSignal(DiagramConfig.createInstructionSignal(null, pio, 0, true, false));
    diagram.addSignal(DiagramConfig.createInstructionSignal(null, pio, 0, true, true));
    //diagram.setSideSetCount(1);
    diagram.create();
  }

  public static void main(final String argv[]) throws IOException
  {
    new Main().run();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
