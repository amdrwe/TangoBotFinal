# TangoBotFinal
DISCLAIMER: This code is very rough and was developed under very tight time constraints, but it works. 
There are many bugs and fatal errors, which will not be fixed for the foreseeable future.

This code powers a robot which navigates autonomously from a starting destination to a set target, using Google's Project Tango. It requires:
  - A robot connected over a serial connection, which responds to 'W','A','S','D', and ' ' (for stop) as commands.
  - The usb-serial-for-android library as a module dependency. It can be found at https://github.com/mik3y/usb-serial-for-android.
  - A tablet/phone with Project Tango compatible hardware.
  - A pre-recorded ADF on the Android device.
