# Arduino Spotify Controller
An arduino HID controller for Spotify via the Spotify Web API.

# Installation/Usage
Keep in mind that this project was strictly intended for my own personal use - it is not designed with user-friendliness in mind whatsoever. The code must be manually edited and compiled in order to be used.
## Arduino

## Authorization
In order for the program to control your Spotify account, it must be provided with an access token. This program does not contain a built-in authorization flow to obtain a token for you - you must obtain this token manually.

The easiest way to do this is to go to [this link](https://developer.spotify.com/console/get-album-tracks/), select GET TOKEN, check all of the boxes, and finally click REQUEST TOKEN. You should be redirected to the same page. The string that appears next to the GET TOKEN button is your access token.

This token must be manually placed in InputListener.java on line 19: `static String access_token = "`\[your token here\]`";`

Keep in mind that this access token only lasts one hour - once it has expired, you must obtain a new one and restart the program.

## Running the Program
Setting this up requires the [Arduino IDE](https://www.arduino.cc/en/software) and a Java IDE. I would recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) in a Maven project, as this is what was used during development.

First, upload the Arduino sketch to the arduino, and then run InputListener.java. Once you see "Started" in the Java output, it should be ready for use.

##  Notes/Known Issues

Any song titles/artists with non-UTF-8 characters will not be displayed properly. This includes any non-english language characters and non-standard symbols. This is due to limitations with both sending serial data to Arduino and the LCD display itself. The display should fix itself upon switching to a new track that can be read properly.

# Credits/Thanks
* [jSerialComm](https://fazecast.github.io/jSerialComm/) Java library, extremely useful for sending/recieving data over serial

* [hd44780](https://www.arduino.cc/reference/en/libraries/hd44780/) Arduino library, extremely useful for using the LCD screen
