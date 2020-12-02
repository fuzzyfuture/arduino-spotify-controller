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

int volume_raw = 0;
int volume = 0;

void setup()
{
  //  set pins to input mode
  for (int i = 0; i < 6; i++)
  {
    pinMode(BUTTON_PINS[i], INPUT_PULLUP);
  }

  //  initialize serial output
  Serial.begin(9600);
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
      Serial.println(BUTTON_NAMES[i]);
      button_last_state[i] = LOW;
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
  }

  delay(100);
}
