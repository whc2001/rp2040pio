/*
 * @(#)GPIOViewPanel.java 1.00 21/05/17
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
package org.soundpaint.rp2040pio.observer.gpio;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.SwingUtils;
import org.soundpaint.rp2040pio.sdk.SDK;

public class GPIOViewPanel extends JPanel
{
  private static final long serialVersionUID = -14152871236331492L;

  public static final ImageIcon ledInLow;
  public static final ImageIcon ledInHigh;
  public static final ImageIcon ledOutLow;
  public static final ImageIcon ledOutHigh;
  public static final ImageIcon ledUnknown;

  static {
    try {
      ledInLow = SwingUtils.createImageIcon("led-green-off16x16.png", "in: 0");
      ledInHigh = SwingUtils.createImageIcon("led-green-on16x16.png", "in: 1");
      ledOutLow = SwingUtils.createImageIcon("led-red-off16x16.png", "out: 0");
      ledOutHigh = SwingUtils.createImageIcon("led-red-on16x16.png", "out: 1");
      ledUnknown = SwingUtils.createImageIcon("led-gray16x16.png", "unknown");
    } catch (final IOException e) {
      final String message =
        String.format("failed loading icon: %s", e.getMessage());
      System.out.println(message);
      throw new InternalError(message, e);
    }
  }

  private final PrintStream console;
  private final SDK sdk;
  private final int refresh;
  private final PIOGPIOArrayPanel pioGpioArrayPanel;
  private final GPIOArrayPanel gpioArrayPanel;
  private int pioNum;

  private GPIOViewPanel()
  {
    throw new UnsupportedOperationException("unsupported empty constructor");
  }

  public GPIOViewPanel(final PrintStream console, final SDK sdk,
                       final int refresh)
    throws IOException
  {
    Objects.requireNonNull(console);
    Objects.requireNonNull(sdk);
    this.console = console;
    this.sdk = sdk;
    this.refresh = refresh;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createTitledBorder("GPIO View"));

    final Box pioSelection = new Box(BoxLayout.X_AXIS);
    add(pioSelection);
    final JLabel lbPio = new JLabel("PIO");
    pioSelection.add(lbPio);
    pioSelection.add(Box.createHorizontalStrut(15));
    addPioButtons(pioSelection);
    pioSelection.add(Box.createHorizontalGlue());
    SwingUtils.setPreferredHeightAsMaximum(pioSelection);

    pioGpioArrayPanel = new PIOGPIOArrayPanel(console, sdk);
    SwingUtils.setPreferredHeightAsMaximum(pioGpioArrayPanel);
    add(pioGpioArrayPanel);

    gpioArrayPanel = new GPIOArrayPanel(console, sdk);
    SwingUtils.setPreferredHeightAsMaximum(gpioArrayPanel);
    add(gpioArrayPanel);

    add(Box.createVerticalGlue());

    pioGpioArrayPanel.pioChanged(pioNum);
    new Thread(() -> updateLoop()).start();
  }

  private void addPioButtons(final Box pioSelection)
  {
    final ButtonGroup bgPio = new ButtonGroup();
    for (int pioNum = 0; pioNum < Constants.PIO_NUM; pioNum++) {
      if (pioNum != 0) pioSelection.add(Box.createHorizontalStrut(10));
      final JRadioButton rbPio = new JRadioButton(String.valueOf(pioNum));
      rbPio.setSelected(pioNum == 0);
      final int finalPioNum = pioNum;
      rbPio.addActionListener((event) -> {
          this.pioNum = finalPioNum;
          pioGpioArrayPanel.pioChanged(finalPioNum);
        });
      bgPio.add(rbPio);
      pioSelection.add(rbPio);
    }
  }

  public void updateLoop()
  {
    final int addressPhase0 =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.
                                  MASTERCLK_TRIGGER_PHASE0);
    final int addressPhase1 =
      PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.
                                  MASTERCLK_TRIGGER_PHASE1);
    final int expectedValue = 0x1; // update upon stable cycle phase 1
    final int mask = 0xffffffff;
    final int cyclesTimeout = 0;
    final int millisTimeout1 = refresh / 2;
    final int millisTimeout2 = refresh - millisTimeout1;
    while (true) {
      try {
        while (true) {
          sdk.wait(addressPhase1, expectedValue, mask,
                   cyclesTimeout, millisTimeout1);
          pioGpioArrayPanel.pioChanged(pioNum);
          pioGpioArrayPanel.repaintLater();
          gpioArrayPanel.update();
          gpioArrayPanel.repaintLater();
          sdk.wait(addressPhase0, expectedValue, mask,
                   cyclesTimeout, millisTimeout2);
        }
      } catch (final IOException e) {
        console.printf("update loop: %s%n", e.getMessage());
        try {
          Thread.sleep(1000); // limit CPU load in case of persisting
                              // error
        } catch (final InterruptedException e2) {
          // ignore
        }
      }
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */