package yetanotherx.dropboxmirrorbot;

import com.beust.jcommander.Parameter;

public class Parameters {
    
    @Parameter(names = { "-user", "-username" }, description = "Username")
    public String username;
    
    @Parameter(names = { "-pass", "-password" }, description = "Password")
    public String password;
    
    @Parameter(names = { "-key" }, description = "Dev key")
    public String key;
    
}
