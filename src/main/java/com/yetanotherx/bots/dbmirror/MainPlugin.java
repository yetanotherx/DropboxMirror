package com.yetanotherx.bots.dbmirror;

import com.beust.jcommander.JCommander;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.yetanotherx.reddit.RedditPlugin;
import com.yetanotherx.reddit.api.data.CommentData;
import com.yetanotherx.reddit.api.data.LinkData;
import com.yetanotherx.reddit.api.modules.ExternalDomain;
import com.yetanotherx.reddit.api.modules.RedditCore;
import com.yetanotherx.reddit.api.modules.RedditLink;
import com.yetanotherx.reddit.api.modules.RedditSubreddit;
import com.yetanotherx.reddit.redditbot.http.Transport;
import com.yetanotherx.reddit.http.request.Request;
import com.yetanotherx.reddit.http.request.RequestType;
import com.yetanotherx.reddit.http.request.WebRequest;
import com.yetanotherx.reddit.http.response.JSONResult;
import com.yetanotherx.reddit.http.response.Response;
import com.yetanotherx.reddit.util.collections.EasyHashMap;

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
            RedditLink newLin = RedditLink.newFromLink(this, link);

            System.out.println("\n--------------------------------------------");
            System.out.println(link.getTitle());
            System.out.println();
            
            try {

                RedditSubreddit sr = RedditSubreddit.newFromName(this, link.getSubreddit());
                System.out.println("Posted to subreddit " + sr.getSubredditData().getDisplayName() + " (" + sr.getSubredditData().getSubscribers() + " subscribers)");
                if (sr.getSubredditData().getSubscribers() < 7500) {
                    throw new DoQuitError("Subreddit does not have enough people to bother rehosting image.");
                }


                for (CommentData dat : newLin.getComments()) {
                    if (dat.getAuthor().equals("dropbox_mirror")) {
                        throw new DoQuitError("I've already posted here! Nothing to do here...");
                    }

                    if (dat.getAuthor().equals("imgur-mirror-bot")) {
                        throw new DoQuitError("Oh no, imgur-mirror-bot beat me!");
                    }
                }


                if (!link.getURL().endsWith(".jpg") && !link.getURL().endsWith(".gif") && !link.getURL().endsWith(".png") && !link.getURL().endsWith(".jpeg")) {
                    throw new DoQuitError("It's not an image. Not gonna go on.");
                }

                Request request = new WebRequest(this);
                request.setURL(link.getURL());
                Transport transport = this.getTransport();
                transport.setRequest(request);

                Response response = transport.sendURL();
                if (response.getHTTPCode().getCode() == 509) {
                    throw new DoQuitError("Oh no! I'm too late! The image already 509'd!");
                }
                
                if( response.getContent().length() > 2000000 ) {
                    throw new DoQuitError("Image is too large to upload to imgur. Image size: " + (response.getContent().length() / 1000000) + " megabytes");
                }

            } catch (DoQuitError e) {
                System.out.println(e.getMessage());
                Thread.sleep(2000);
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                Thread.sleep(2000);
                continue;
            }

            System.out.println("Looks like we can upload to imgur! Link: " + link.getURL());

            HashMap<String, String> map = new EasyHashMap<String, String>(
                    "image", link.getURL(),
                    "title", link.getTitle(),
                    "key", key,
                    "type", "url");

            Request req = new WebRequest(this, map);
            req.setURL("http://api.imgur.com/2/upload.json");
            req.setMethod(RequestType.POST);
            Transport tp = this.getTransport();
            tp.setRequest(req);

            Response resp = null;
            try {
                resp = tp.sendURL();
                JSONResult json = resp.getJSONResult();
                String url = json.getString("upload/links/original");

                if (url != null) {
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

                    System.out.println("All done! Sleeping for 10 minutes");
                    Thread.sleep(600000);//10 minutes

                } else {
                    System.out.println("Something went wrong....");
                    System.out.println(resp.getContent());
                }
            } catch (Exception e) {
                System.out.println("Something went wrong....");
                if (resp != null) {
                    System.out.println(resp.getContent());
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
