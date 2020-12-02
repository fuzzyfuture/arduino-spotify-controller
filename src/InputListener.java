import java.io.IOException;
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
    static String access_token = "BQAGpcoBACht5j5-TmSTLPVE-Znx3gRETrstxXupb6gZdfzaFqAVhhjwM4FksUfWJJfdq6kt2E1A7ZXkizW3-CGVlokyNR_LVwJNYTFijKkc8DFZgPVfsiFwt3lQ87fLL5GqpatUOGeLp6K_ps2wtlCIIgjbqOg5gmeVFYB0X8Vw4QDx_-w3PnTa8uXOku_iK-9twLH8nTGNmbZHX9qrPtSY5gxBFzh246Q8ki8KjMZ15_dAYzv3WlUXjayN7WmyC5p_-mFZ__ceOokOOJjw";

    public String getRequest(String uri, CloseableHttpClient http_client, ResponseHandler<String> response_handler)
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

        String input = input_raw.substring(0, input_raw.indexOf(":"));
        System.out.println(input);

        CloseableHttpClient http_client = HttpClients.createDefault();
        ResponseHandler<String> response_handler = response->
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

        if (player_info != null)
        {
            shuffle_state = player_info.getBoolean("shuffle_state");
            loop_state = player_info.getString("repeat_state");
            playing_state = player_info.getBoolean("is_playing");
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

    static public void main(String[] args)
    {
        SerialPort port = SerialPort.getCommPort("COM3");
        port.openPort();

        InputListener listener = new InputListener();
        port.addDataListener(listener);

        System.out.println("Started");
    }
}