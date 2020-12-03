#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>

hd44780_I2Cexp lcd;
const int LCD_COLS = 16;
const int LCD_ROWS = 2;

//  pins and names of buttons
const int BUTTON_PINS[6] = {7, 8, 9, 10, 11, 12};
const String BUTTON_NAMES[6] = {"Shuffle:", "Loop:", "Prev:", "Play:", "Next:", "Mute:"};

//  pin of potentiometer
const int VOLUME_PIN = A0;
const int VOLUME_THRES = 5;

// stores last state of button (initialized to 0)
int button_last_state[6] = {1, 1, 1, 1, 1, 1};

//  stores last state of potentiometer
int volume_last_state = 0;

//  stores current state of button for loop
int button_current_state = 0;

String song_current_name = "";

int volume_raw = 0;
int volume = 0;

String button_name = "";

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

  lcd.begin(LCD_COLS, LCD_ROWS);
  
  serial_input.reserve(200);
}

void loop()
{ 
  //  looping through all buttons
  for (int i = 0; i < 6; i++)
  {
    //  getting current state of button
    button_current_state = digitalRead(BUTTON_PINS[i]);

    if (button_last_state[i] == HIGH && button_current_state == LOW)
    {
      button_name = BUTTON_NAMES[i];
      
      Serial.println(button_name);
      button_last_state[i] = LOW;

      lcd.clear();
 
      lcd.setCursor(0, 1);
      lcd.print(button_name.substring(0, button_name.indexOf(":")));

      delay(300);

      lcd.clear();
    }
    else if (button_last_state[i] == LOW && button_current_state == HIGH)
    {
      button_last_state[i] = HIGH;
    }
  }

  volume_raw = analogRead(VOLUME_PIN);
  volume = map(volume_raw, 0, 1023, 0, 100);

  if (volume >= volume_last_state + VOLUME_THRES || volume <= volume_last_state - VOLUME_THRES)
  {
    Serial.print("Volume:");
    Serial.println(volume);
    
    volume_last_state = volume;

    lcd.clear();

    lcd.setCursor(0, 1);
    lcd.print("Volume: ");
    lcd.print(volume);

    delay(300);

    lcd.clear();
  }

  if (serial_input_complete && song_current_name != serial_input)
  {
    song_current_name = serial_input;

    serial_input = "";
    serial_input_complete = false;

    lcd.clear();
  }

  lcd.setCursor(0, 0);
  lcd.print(song_current_name.substring(song_current_name.indexOf("-") + 2));

  lcd.setCursor(0, 1);
  lcd.print(song_current_name.substring(0, song_current_name.indexOf("-") - 1));
}

void serialEvent()
{
  while (Serial.available())
  {
    char in = (char)Serial.read();

    if (in == '\n')
    {
      serial_input_complete = true;
      break;
    }

    serial_input += in;
  }
}
