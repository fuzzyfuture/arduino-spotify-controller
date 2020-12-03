import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.*;

import com.fazecast.jSerialComm.*;

public final class InputListener implements SerialPortMessageListener
{
    static String access_token = "BQD6zg59xkjuH6kZDryfQ7tRJ-SI-exXR6t8HtbE4445kHC4DaO8LAJxJ6K3Uw5fjXDaVe4YX_oWJxs6iy2X5w18g9EqM52t5gtNyB_Fub08Ph-PwLzaVOD4v06Y1NI-WJ_scaFLt6fpjqGxmgLy5OUbF2USiImcBbdYMppkSp-COvkGPiDvNLjT4TJie6g4AvjfrFDxRoMQx3Tji3S9SSLaH7hLSBcWg5t5pr5cWPvDxI4vDAX36lB7mtEeFTxqpXQlpv_QtQYTyrOIO_79";
    static SerialPort port;
    static CloseableHttpClient http_client = HttpClients.createDefault();

    static ResponseHandler<String> response_handler = response->
    {
        int status = response.getStatusLine().getStatusCode();
        String msg = response.getStatusLine().getReasonPhrase();
        String data = "";

        HttpEntity entity = response.getEntity();
        if (entity != null)
        {
            data = EntityUtils.toString(entity);
        }

        return "Status: " + status + "\n" + "Response: " + msg + "\n" + "Data: " + data;
    };

    public static String getRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
    {
        HttpGet req =  new HttpGet(uri);

        req.setHeader("Accept", "application/json");
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Authorization", "Bearer " + access_token);

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

    public String putRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
    {
        HttpPut req =  new HttpPut(uri);

        req.setHeader("Accept", "application/json");
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Authorization", "Bearer " + access_token);

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

    public String postRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
    {
        HttpPost req =  new HttpPost(uri);

        req.setHeader("Accept", "application/json");
        req.setHeader("Content-Type", "application/json");
        req.setHeader("Authorization", "Bearer " + access_token);

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

    @Override
    public byte[] getMessageDelimiter()
    {
        return new byte[]
                {
                        (byte)0x0A
                };
    }

    @Override
    public boolean delimiterIndicatesEndOfMessage()
    {
        return true;
    }

    @Override
    public void serialEvent(SerialPortEvent event)
    {
        String input_raw = new String(event.getReceivedData(), StandardCharsets.UTF_8);
        String input = "";

        if (input_raw.contains(":"))
        {
            input = input_raw.substring(0, input_raw.indexOf(":"));
            System.out.println(input);
        }
        else
        {
            return;
        }

        String player_info_raw = getRequest("https://api.spotify.com/v1/me/player", http_client, response_handler);
        JSONObject player_info = null;

        if (player_info_raw != null)
        {
            player_info_raw = player_info_raw.substring(player_info_raw.indexOf("{") - 1);
            player_info = new JSONObject(player_info_raw);
        }

        boolean shuffle_state = false;
        String loop_state = "off";
        boolean playing_state = false;
        String song_data = "";

        if (player_info != null)
        {
            shuffle_state = player_info.getBoolean("shuffle_state");
            loop_state = player_info.getString("repeat_state");
            playing_state = player_info.getBoolean("is_playing");
            song_data = player_info.getJSONObject("item").getJSONArray("artists").getJSONObject(0).getString("name") + " - " + player_info.getJSONObject("item").getString("name") + "\n";
        }

        switch (input)
        {
            case "Shuffle":
                System.out.println(putRequest("https://api.spotify.com/v1/me/player/shuffle?state=" + !shuffle_state, http_client, response_handler));
                break;
            case "Loop":
                String new_loop_state;

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

                System.out.println(putRequest("https://api.spotify.com/v1/me/player/repeat?state=" + new_loop_state, http_client, response_handler));
                break;
            case "Prev":
                System.out.println(postRequest("https://api.spotify.com/v1/me/player/previous", http_client, response_handler));
                break;
            case "Play":
                String uri;

                if (playing_state)
                {
                    uri = "https://api.spotify.com/v1/me/player/pause";
                }
                else
                {
                    uri = "https://api.spotify.com/v1/me/player/play";
                }

                System.out.println(putRequest(uri, http_client, response_handler));
                break;
            case "Next":
                System.out.println(postRequest("https://api.spotify.com/v1/me/player/next", http_client, response_handler));
                break;
            case "Volume":
                String volume = input_raw.substring(input_raw.indexOf(":") + 1, input_raw.indexOf("\n") - 1);
                System.out.println("Volume is: " + volume);
                System.out.println(putRequest("https://api.spotify.com/v1/me/player/volume?volume_percent=" + volume, http_client, response_handler));
                break;
            default:
                System.out.println("Invalid input.");
                break;
        }
    }

    static public void main(String[] args) throws InterruptedException {
        port = SerialPort.getCommPort("COM3");
        port.openPort();
        port.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

        InputListener listener = new InputListener();
        port.addDataListener(listener);

        Thread.sleep(2000);

        System.out.println("Started");

        String last_song = "";

        while (true)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            String player_info_raw = getRequest("https://api.spotify.com/v1/me/player", http_client, response_handler);
            JSONObject player_info = null;

            if (player_info_raw != null)
            {
                player_info_raw = player_info_raw.substring(player_info_raw.indexOf("{") - 1);
                player_info = new JSONObject(player_info_raw);
            }

            String song_data = "";

            if (player_info != null)
            {
                song_data = player_info.getJSONObject("item").getJSONArray("artists").getJSONObject(0).getString("name") + " - " + player_info.getJSONObject("item").getString("name") + "\n";
            }

            if (!song_data.equals(last_song))
            {
                last_song = song_data;
                port.writeBytes(song_data.getBytes(), song_data.length());
            }
        }
    }
}