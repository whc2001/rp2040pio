/*
 * @(#)GPIO.java 1.00 21/01/31
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

/**
 * General-Purpose Set of 32 Peripheral I/O Terminals
 */
public class GPIO
{
  public enum Function {
    XIP(0, "xip"),
    SPI(1, "spi"),
    UART(2, "uart"),
    I2C(3, "i2c"),
    PWM(4, "pwm"),
    SIO(5, "sio"),
    PIO0(6, "pio0"),
    PIO1(7, "pio1"),
    GPCK(8, "gpck"),
    USB(9, "usb"),
    NULL(15, "null");

    private final int value;
    private final String label;

    private Function(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    private int getValue() { return value; }

    @Override
    public String toString()
    {
      return label;
    }
  };

  public enum Direction {
    IN(0, "in"),
    OUT(1, "out");

    private final int value;
    private final String label;

    private Direction(final int value, final String label)
    {
      this.value = value;
      this.label = label;
    }

    private int getValue() { return value; }

    @Override
    public String toString()
    {
      return label;
    }
  };

  private static Direction directionFromValue(final int value)
  {
    if (value == 0)
      return Direction.IN;
    else if (value == 1)
      return Direction.OUT;
    throw new IllegalArgumentException("value is neither 0 nor 1: " + value);
  }

  private static Bit bitFromValue(final int value)
  {
    if (value == 0)
      return Bit.LOW;
    else if (value == 1)
      return Bit.HIGH;
    throw new IllegalArgumentException("value is neither 0 nor 1: " + value);
  }

  private static class Terminal
  {
    private Function function;
    private Direction direction;
    private Bit value;

    public void reset()
    {
      function = Function.NULL;
      direction = Direction.IN;
      value = Bit.LOW;
    }

    public char toChar()
    {
      if (direction == Direction.IN) {
        return value == Bit.HIGH ? '¹' : '⁰';
      } else {
        return value == Bit.HIGH ? '₁' : '₀';
      }
    }
  }

  private final Terminal[] terminals;
  private int regINPUT_SYNC_BYPASS; // bits 0..31 of INPUT_SYNC_BYPASS
                                    // (contents currently ignored)

  public GPIO()
  {
    terminals = new Terminal[Constants.GPIO_NUM];
    for (int port = 0; port < terminals.length; port++) {
      terminals[port] = new Terminal();
    }
    reset();
  }

  public void reset()
  {
    for (int port = 0; port < terminals.length; port++) {
      final Terminal terminal = terminals[port];
      terminal.function = Function.NULL;
      terminal.direction = Direction.IN;
      terminal.value = Bit.LOW;
    }
  }

  /**
   * Set GPIOx_CTRL_FUNCSEL to 6 (for PIO0) or 7 (for PIO1), see
   * Sect. 2.19.2. "Function Select" of RP2040 datasheet for details.
   */
  public void setFunction(final int gpio, final Function fn)
  {
    if ((gpio < 0) || (gpio >= terminals.length)) {
      throw new IllegalArgumentException("gpio port out of range: " + gpio);
    }
    if (fn == null) {
      throw new NullPointerException("fn");
    }
    terminals[gpio].function = fn;
  }

  public void setBit(final int port, final Bit value)
  {
    // TODO: Clarify what happens when writing to a GPIO with pin
    // direction set to IN.
    if (value == null) {
      throw new NullPointerException("value");
    }
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    terminals[port].value = value;
  }

  public Bit getBit(final int port)
  {
    // TODO: Clarify what happens when reading from a GPIO with pin
    // direction set to OUT.
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    return terminals[port].value;
  }

  public void setDirection(final int port, final Direction direction)
  {
    if (direction == null) {
      throw new NullPointerException("direction");
    }
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    terminals[port].direction = direction;
  }

  public Direction getDirection(final int port)
  {
    if ((port < 0) || (port >= terminals.length)) {
      throw new IllegalArgumentException("port out of range: " + port);
    }
    return terminals[port].direction;
  }

  public int getPins(final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin base > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin count > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         count);
    }
    int pins = 0;
    for (int pin = 0; pin < count; pin++) {
      pins = (pins << 0x1) | getBit((base + pin) & 0x1f).getValue();
    }
    return pins;
  }

  public void setPins(final int pins, final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin base > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin count > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         count);
    }
    for (int pin = 0; pin < count; pin++) {
      setBit((base + pin) & 0x1f, bitFromValue((pins >>> pin) & 0x1));
    }
  }

  public int getPinDirs(final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin base > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin count > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         count);
    }
    int pinDirs = 0;
    for (int pin = 0; pin < count; pin++) {
      pinDirs = (pinDirs << 0x1) | getDirection((base + pin) & 0x1f).getValue();
    }
    return pinDirs;
  }

  public void setPinDirs(final int pinDirs, final int base, final int count)
  {
    if (base < 0) {
      throw new IllegalArgumentException("GPIO pin base < 0: " + base);
    }
    if (base > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin base > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         base);
    }
    if (count < 0) {
      throw new IllegalArgumentException("GPIO pin count < 0: " + count);
    }
    if (count > Constants.GPIO_NUM - 1) {
      throw new IllegalArgumentException("GPIO pin count > " +
                                         (Constants.GPIO_NUM - 1) + ": " +
                                         count);
    }
    for (int pin = 0; pin < count; pin++) {
      setDirection((base + pin) & 0x1f,
                   directionFromValue((pinDirs >>> pin) & 0x1));
    }
  }

  public void setInputSyncByPass(final int bits)
  {
    regINPUT_SYNC_BYPASS = bits;
  }

  public int getInputSyncByPass()
  {
    return regINPUT_SYNC_BYPASS;
  }

  public String asBitArrayDisplay()
  {
    final StringBuffer s = new StringBuffer();
    for (final Terminal terminal : terminals) {
      s.append(terminal.toChar());
      if ((s.length() + 1) % 9 == 0) s.append(' ');
    }
    return s.toString();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:Java
 * End:
 */
