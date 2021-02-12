/*
 * @(#)PLL.java 1.00 21/02/05
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

import java.util.ArrayList;
import java.util.List;

/**
 * Phase Locked Loop (PLL)
 */
public class PLL implements Clock.TransitionListener
{
  private int regCLKDIV_INT; // bits 16..31 of SMx_CLKDIV
  private int regCLKDIV_FRAC; // bits 8..15 of SMx_CLKDIV
  private int countIntegerBits;
  private int countFractionalBits;
  private boolean clockEnable;

  public PLL()
  {
    reset();
  }

  public void reset()
  {
    countIntegerBits = 0x0001;
    countFractionalBits = 0x00;
    clockEnable = false;
  }

  public int getDivIntegerBits()
  {
    return regCLKDIV_INT;
  }

  public void setDivIntegerBits(final int divIntegerBits)
  {
    if (divIntegerBits < 0) {
      throw new IllegalArgumentException("div integer bits < 0: " +
                                         divIntegerBits);
    }
    if (divIntegerBits > 0xffff) {
      throw new IllegalArgumentException("div integer bits > 65535: " +
                                         divIntegerBits);
    }
    this.regCLKDIV_INT = divIntegerBits;
  }

  public int getDivFractionalBits()
  {
    return regCLKDIV_FRAC;
  }

  public void setDivFractionalBits(final int divFractionalBits)
  {
    if (divFractionalBits < 0) {
      throw new IllegalArgumentException("div fractional bits < 0: " +
                                         divFractionalBits);
    }
    if (divFractionalBits > 0xff) {
      throw new IllegalArgumentException("div fractional bits > 255: " +
                                         divFractionalBits);
    }
    this.regCLKDIV_FRAC = divFractionalBits;
  }

  public void setCLKDIV(final int clkdiv)
  {
    setDivIntegerBits(clkdiv >>> 16);
    setDivFractionalBits((clkdiv >>> 8) & 0xff);
  }

  public int getCLKDIV()
  {
    return
      (getDivIntegerBits() << 16) |
      (getDivFractionalBits() << 8);
  }

  public boolean getClockEnable()
  {
    return clockEnable;
  }

  @Override
  public void raisingEdge(final long wallClock)
  {
    /*
     * TODO: Clarify: Sect. 3.5.5. "Clock Dividers", Fig. 46: "clock
     * divider ... emits an enable pulse when it reaches 1"
     *
     * -- Really "1", not "0"?
     */
    if (countIntegerBits <= 1) {
      countIntegerBits += regCLKDIV_INT;
      countFractionalBits += regCLKDIV_FRAC;
      if (countFractionalBits >= 0x10000) {
        countFractionalBits -= 0x10000;
        countIntegerBits++;
      }
      clockEnable = true;
    } else {
      clockEnable = false;
    }
    countIntegerBits--;
  }

  @Override
  public void fallingEdge(final long wallClock) {}
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
