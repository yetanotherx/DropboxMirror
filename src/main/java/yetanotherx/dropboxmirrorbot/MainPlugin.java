package yetanotherx.dropboxmirrorbot;

import com.beust.jcommander.JCommander;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import yetanotherx.redditbot.RedditPlugin;
import yetanotherx.redditbot.api.data.CommentData;
import yetanotherx.redditbot.api.data.LinkData;
import yetanotherx.redditbot.api.modules.ExternalDomain;
import yetanotherx.redditbot.api.modules.RedditCore;
import yetanotherx.redditbot.api.modules.RedditLink;
import yetanotherx.redditbot.http.Transport;
import yetanotherx.redditbot.http.request.Request;
import yetanotherx.redditbot.http.request.RequestType;
import yetanotherx.redditbot.http.request.WebRequest;
import yetanotherx.redditbot.http.response.JSONResult;
import yetanotherx.redditbot.http.response.Response;
import yetanotherx.redditbot.util.collections.EasyHashMap;

public class MainPlugin extends RedditPlugin {

    private Parameters params;

    public MainPlugin(Parameters params) {
        this.params = params;
    }

    public void run() throws InterruptedException {
        RedditCore.newFromUserAndPass(this, params.username, params.password).doLogin();
        String key = new StringBuilder().append(params.key).toString();
        params = null;

        ExternalDomain dom = ExternalDomain.newFromDomain(this, "dl.dropbox.com");
        for (LinkData link : dom.getUsages()) {

            System.out.println("--------------------------------------------");

            RedditLink newLin = RedditLink.newFromLink(this, link);
            System.out.println("Starting to parse: " + newLin.getLinkData().getTitle());

            boolean quit = false;
            for (CommentData dat : newLin.getComments()) {
                if (dat.getAuthor().equals("dropbox_mirror") || dat.getAuthor().equals("imgur-mirror-bot")) {
                    System.out.println("I've been beat! (or I already posted...)");
                    quit = true;
                }
            }

            if (quit) {
                Thread.sleep(2000);
                continue;
            }

            Transport transport = this.getTransport();

            HashMap<String, String> map = new EasyHashMap<String, String>(
                    "image", link.getURL(),
                    "title", link.getTitle(),
                    "key", key,
                    "type", "url");

            if (!link.getURL().endsWith(".jpg") && !link.getURL().endsWith(".gif") && !link.getURL().endsWith(".png") && !link.getURL().endsWith(".jpeg")) {
                System.out.println("It's not an image. Not gonna go on.");
                Thread.sleep(2000);
                continue;
            }

            System.out.println("Uploading " + link.getURL() + " to imgur!");
            Request request = new WebRequest(this, map);
            request.setURL("http://api.imgur.com/2/upload.json");
            request.setMethod(RequestType.POST);
            transport.setRequest(request);

            Response response = null;
            try {
                response = transport.sendURL();
                JSONResult json = response.getJSONResult();
                if (json.getString("upload/links/original") != null) {

                    String url = json.getString("upload/links/original");

                    System.out.println("Upload successful! URL: " + url);
                    StringBuilder text = new StringBuilder();
                    text.append("Image rehosted on to Imgur from Dropbox. (BETA - [why?](http://www.reddit.com/r/DropboxMirror/comments/njpab/about_dropbox_mirror_reddit_bot/))");
                    text.append("\n\n");
                    text.append("New image: [");
                    text.append(url);
                    text.append("](");
                    text.append(url);
                    text.append(")\n\n");
                    text.append("*Was this a mistake, or is there a bug? Message [yetanotherx](http://reddit.com/user/yetanotherx)!*");
                    newLin.doReply(text.toString());
                    Thread.sleep(600000);//10 minutes
                } else {
                    System.out.println("Something went wrong....");
                    System.out.println(response.getContent());
                }
            } catch (Exception e) {
                System.out.println("Something went wrong....");
                if (response != null) {
                    System.out.println(response.getContent());
                }
                e.printStackTrace();
            }

            Thread.sleep(2000);
        }
    }

    @Override
    public String getName() {
        return "DropboxMirrorBot";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    public static void main(String[] args) {
        Parameters params = new Parameters();
        JCommander jc = new JCommander(params, args);
        try {
            new MainPlugin(params).run();
        } catch (InterruptedException ex) {
            Logger.getLogger(MainPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
