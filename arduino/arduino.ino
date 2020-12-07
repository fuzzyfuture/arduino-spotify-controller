#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>

//  2 x 16 display
hd44780_I2Cexp lcd;
const int LCD_COLS = 16;
const int LCD_ROWS = 2;

//  pins and names of buttons
const int BUTTON_PINS[6] = {7, 8, 9, 10, 11, 12};
const String BUTTON_NAMES[6] = {"Shuffle:", "Loop:", "Prev:", "Play:", "Next:", "Mute:"};

//  pin of potentiometer
const int VOLUME_PIN = A0;
//  volume update will only be sent if new volume is higher or lower than the current volume by this amount
const int VOLUME_THRES = 5;

// stores last state of button (initialized to 0)
int button_last_state[6] = {1, 1, 1, 1, 1, 1};

//  stores last state of potentiometer
int volume_last_state = 0;

//  stores current state of button for loop
int button_current_state = 0;

//  stores name of currently playing song
String song_current_name = "";

//  raw input from potentiometer and converted volume
int volume_raw = 0;
int volume = 0;

//  stores name of current button for loop
String button_name = "";

//  stores last input from serial
String serial_input = "";
bool serial_input_complete = false;

void setup()
{ 
  //  set pins to input mode
  for (int i = 0; i < 6; i++)
  {
    pinMode(BUTTON_PINS[i], INPUT_PULLUP);
  }

  //  initialize serial output
  Serial.begin(9600);

  //  start lcd
  lcd.begin(LCD_COLS, LCD_ROWS);

  //  reserve 
  serial_input.reserve(1000);
}

void loop()
{ 
  //  looping through all buttons
  for (int i = 0; i < 6; i++)
  {
    //  getting current state of button
    button_current_state = digitalRead(BUTTON_PINS[i]);

    //  button pressed
    if (button_last_state[i] == HIGH && button_current_state == LOW)
    {
      button_name = BUTTON_NAMES[i];

      // send name to serial and update current button state
      Serial.println(button_name);
      button_last_state[i] = LOW;

      //  print button name to the lower portion of the lcd
      lcd.clear();
 
      lcd.setCursor(0, 1);
      lcd.print(button_name.substring(0, button_name.indexOf(":")));

      //  only want to show temporarily
      delay(300);

      lcd.clear();
    }

    //  button released
    else if (button_last_state[i] == LOW && button_current_state == HIGH)
    {
      button_last_state[i] = HIGH;
    }
  }

  //  reading volume from potentiometer
  volume_raw = analogRead(VOLUME_PIN);
  //  spotify handles volume via percentage (0%-100%) so we convert the raw reading
  volume = map(volume_raw, 0, 1023, 0, 100);

  //  threshold prevents instability (e.g. volume rapidly going between 72 and 73) due to inaccuracy of potentiometer
  if (volume >= volume_last_state + VOLUME_THRES || volume <= volume_last_state - VOLUME_THRES)
  {
    //  send the new volume
    Serial.print("Volume:");
    Serial.println(volume);

    //  update state
    volume_last_state = volume;

    //  print to lower portion of lcd
    lcd.clear();

    lcd.setCursor(0, 1);
    lcd.print("Volume: ");
    lcd.print(volume);

    //  only want to show temporarily
    delay(300);

    lcd.clear();
  }

  //  checking if new song name recieved via serial is different
  //  (if not, it is unneccessary to update the lcd)
  if (serial_input_complete && song_current_name != serial_input)
  {
    //  updating current song
    song_current_name = serial_input;

    //  resetting serial input
    serial_input = "";
    serial_input_complete = false;

    //  clear lcd
    lcd.clear();
  }

  //  print current song name to the top line of the lcd
  //  and the current artist to the bottom line
  lcd.setCursor(0, 0);
  lcd.print(song_current_name.substring(song_current_name.indexOf("-") + 2));

  lcd.setCursor(0, 1);
  lcd.print(song_current_name.substring(0, song_current_name.indexOf("-") - 1));
}

//  this function is automatically triggered every loop()
//  when there is serial data to be read
//  https://www.arduino.cc/en/Tutorial/BuiltInExamples/SerialEvent
void serialEvent()
{
  while (Serial.available())
  {
    //  get next character
    char in = (char)Serial.read();

    //  if next character is a new line, the string is finished
    if (in == '\n')
    {
      serial_input_complete = true;
      break;
    }

    //  add next character to string
    serial_input += in;
  }
}
