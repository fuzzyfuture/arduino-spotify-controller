import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.*;

import com.fazecast.jSerialComm.*;

public final class InputListener implements SerialPortMessageListener
{
    //  authorization to access spotify api - see readme for more details
    static String access_token = "";
    //  serial port to connect to
    static SerialPort port;
    //  allows us to send/recieve http requests
    static CloseableHttpClient http_client = HttpClients.createDefault();

    //  handles the response from http requests
    static ResponseHandler<String> response_handler = response->
    {
        //  response consists of three parts:
        //  status code (e.g. 404)
        int status = response.getStatusLine().getStatusCode();
        //  reason phrase (e.g. file not found)
        String msg = response.getStatusLine().getReasonPhrase();
        //  response data (e.g. some json data)
        String data = "";

        //  only want to attempt to get data if our request returns it
        HttpEntity entity = response.getEntity();
        if (entity != null)
        {
            data = EntityUtils.toString(entity);
        }

        //  complete response
        return "Status: " + status + "\n" + "Response: " + msg + "\n" + "Data: " + data;
    };

    //  submits an http get request
    public static String getRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
    {
        //  setting up the request and formatting for spotify's api
        HttpGet req =  new HttpGet(uri);

        req.setHeader("Accept", "application/json");
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Authorization", "Bearer " + access_token);

        //  send the request
        try
        {
            return http_client.execute(req, response_handler);
        }
        catch (IOException e)
        {
            System.out.println("Error: " + e.getMessage());

            return null;
        }
    }

    //  submits an http put request
    public String putRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
    {
        //  setting up the request and formatting for spotify's api
        HttpPut req =  new HttpPut(uri);

        req.setHeader("Accept", "application/json");
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Authorization", "Bearer " + access_token);

        //  send the request
        try
        {
            return http_client.execute(req, response_handler);
        }
        catch (IOException e)
        {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();

            return null;
        }
    }

    //  submits an http post request
    public String postRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
    {
        //  setting up the request and formatting for spotify's api
        HttpPost req =  new HttpPost(uri);

        req.setHeader("Accept", "application/json");
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Authorization", "Bearer " + access_token);

        //  send the request
        try
        {
            return http_client.execute(req, response_handler);
        }
        catch (IOException e)
        {
            System.out.println("Error: " + e.getMessage());

            return null;
        }
    }

    @Override
    public int getListeningEvents()
    {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    //  data read from serial should be delimited by a new line (0x0A)
    @Override
    public byte[] getMessageDelimiter()
    {
        return new byte[]
                {
                        (byte)0x0A
                };
    }

    //  indeed it does
    @Override
    public boolean delimiterIndicatesEndOfMessage()
    {
        return true;
    }

    //  this function is automatically triggered every time something is sent/received over serial
    @Override
    public void serialEvent(SerialPortEvent event)
    {
        //  serial input needs to be converted from array of bytes to UTF-8 string
        String input_raw = new String(event.getReceivedData(), StandardCharsets.UTF_8);
        String input = "";

        //  valid requests sent from arduino all end with a ":"
        if (input_raw.contains(":"))
        {
            //  removing the ":"
            input = input_raw.substring(0, input_raw.indexOf(":"));
            System.out.println(input);
        }
        else
        {
            return;
        }

        //  api request to get info about the current status of the player
        //  contains info like what song is playing, whether shuffle is on, etc.
        String player_info_raw = getRequest("https://api.spotify.com/v1/me/player", http_client, response_handler);
        JSONObject player_info = null;

        if (player_info_raw != null)
        {
            //  get request function returns the http status code and response as well as the json data
            //  so we cut it down to only the json data
            player_info_raw = player_info_raw.substring(player_info_raw.indexOf("{") - 1);
            player_info = new JSONObject(player_info_raw);
        }

        //  initializing some status variables
        boolean shuffle_state = false;
        String loop_state = "off";
        boolean playing_state = false;
        String song_data = "";

        //  if data was received, we grab the data we need and put it into a corresponding variable
        if (player_info != null)
        {
            shuffle_state = player_info.getBoolean("shuffle_state");
            loop_state = player_info.getString("repeat_state");
            playing_state = player_info.getBoolean("is_playing");
            song_data = player_info.getJSONObject("item").getJSONArray("artists").getJSONObject(0).getString("name") + " - " + player_info.getJSONObject("item").getString("name") + "\n";
        }

        //  now looking at serial input from arduino
        switch (input)
        {
            case "Shuffle":
                //  api request to set shuffle to the opposite of what it currently is
                System.out.println(putRequest("https://api.spotify.com/v1/me/player/shuffle?state=" + !shuffle_state, http_client, response_handler));
                break;
            case "Loop":
                String new_loop_state;

                //  go to next loop state (off -> track -> context ->)
                switch (loop_state)
                {
                    case "off":
                        new_loop_state = "track";
                        break;
                    case "track":
                        new_loop_state = "context";
                        break;
                    default:
                        new_loop_state = "off";
                        break;
                }

                //  api request to send the new loop state
                System.out.println(putRequest("https://api.spotify.com/v1/me/player/repeat?state=" + new_loop_state, http_client, response_handler));
                break;
            case "Prev":
                //  api request to go to prev track
                System.out.println(postRequest("https://api.spotify.com/v1/me/player/previous", http_client, response_handler));
                break;
            case "Play":
                String uri;

                //  different uri to either play or pause song
                //  if song is playing, we want to pause it
                //  if song is paused, we want to play it
                if (playing_state)
                {
                    uri = "https://api.spotify.com/v1/me/player/pause";
                }
                else
                {
                    uri = "https://api.spotify.com/v1/me/player/play";
                }

                //  send api request
                System.out.println(putRequest(uri, http_client, response_handler));
                break;
            case "Next":
                //  sent api request to go to next track
                System.out.println(postRequest("https://api.spotify.com/v1/me/player/next", http_client, response_handler));
                break;
            case "Volume":
                //  volume input will look something like "Volume:58" so we need to trim everything except the number itself
                String volume = input_raw.substring(input_raw.indexOf(":") + 1, input_raw.indexOf("\n") - 1);
                System.out.println("Volume is: " + volume);
                //  api request to change volume
                System.out.println(putRequest("https://api.spotify.com/v1/me/player/volume?volume_percent=" + volume, http_client, response_handler));
                break;
            default:
                //  sometimes when loading the arduino it sends weird inputs
                System.out.println("Invalid input.");
                break;
        }
    }

    static public void main(String[] args) throws InterruptedException {
        //  setting up serial port
        //  arduino is on COM3
        port = SerialPort.getCommPort("COM3");
        port.openPort();
        //  arduino is set to 9600
        port.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

        InputListener listener = new InputListener();
        port.addDataListener(listener);

        //  need to wait 2 seconds-ish to make sure the arduino is ready to receive data
        //  not sure why this works
        Thread.sleep(2000);

        System.out.println("Started");

        String last_song = "";

        //  infinite loop to send what song is currently playing
        while (true)
        {
            //  loops every 1 second
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            //  get current player info
            String player_info_raw = getRequest("https://api.spotify.com/v1/me/player", http_client, response_handler);
            JSONObject player_info = null;

            //  trim everything except json data
            if (player_info_raw != null)
            {
                player_info_raw = player_info_raw.substring(player_info_raw.indexOf("{") - 1);
                player_info = new JSONObject(player_info_raw);
            }

            String song_data = "";

            //  get the current song and artist
            if (player_info != null)
            {
                song_data = player_info.getJSONObject("item").getJSONArray("artists").getJSONObject(0).getString("name") + " - " + player_info.getJSONObject("item").getString("name") + "\n";
            }

            //  if the song has changed
            if (!song_data.equals(last_song))
            {
                //  write to serial
                last_song = song_data;
                port.writeBytes(song_data.getBytes(), song_data.length());
            }
        }
    }
}