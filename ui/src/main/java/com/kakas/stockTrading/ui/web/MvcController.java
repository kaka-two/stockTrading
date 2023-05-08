package com.kakas.stockTrading.ui.web;

import com.kakas.stockTrading.client.RestClient;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.dbService.UserProfileServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.raw.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@Slf4j
public class MvcController {
    @Value("#{TradingConfiguration.hmacKey}")
    String hmacKey;

    @Autowired
    CookieService cookieService;

    @Autowired
    UserProfileServiceImpl userService;

    @Autowired
    RestClient restClient;

    @Autowired
    Environment environment;

    @PostConstruct
    public void init() {
        if (isLocalEnv()) {
            for (int i = 0; i < 10; i++) {
                String email = "user" + i + "@outlook.com";
                String name = "user" + i;
                String password = "password" + i;
                if (userService.getUserProfileByEmail(email) == null) {
                    log.info("auto create user {} for local environment.", email);
                    doSignup(email, name, password);
                }

            }
        }
    }

    @GetMapping("/")
    public ModelAndView index(){
        if(UserContext.getUserId() == null) {
            return redirect("/signin");
        }
        return prepareModelAndView("index");
    }

    @GetMapping("/signup")
    public ModelAndView signup() {
        if(UserContext.getUserId() == null) {
            return redirect("/");
        }
        return prepareModelAndView("signup");
    }

    @PostMapping("/signup")
    public ModelAndView signup(@RequestParam("email") String email, @RequestParam("name") String name, @RequestParam("password") String password) {
        // check email
        if (email == null || email.isEmpty()) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid Email"));
        }
        email = email.trim().toLowerCase();
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid Email"));
        }
        if (userService.getUserProfileByEmail(email) != null) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Email existed"));
        }
        // check name
        if (name == null || name.isEmpty() || name.trim().length() > 100) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid Name"));
        }
        name = name.trim();
        // check password
        if (password == null ||  password.length() < 8 || password.length() > 32) {
            return prepareModelAndView("signup", Map.of("email", email, "name", name, "error", "Invalid Password"));
        }
        doSignup(email, name, password);
        return redirect("/signin");
    }

    @GetMapping("/signin")
    public ModelAndView signin() {

    }

    @PostMapping("/signin")
    public ModelAndView signin(String email, String password) {

    }

    @GetMapping("/signout")
    public ModelAndView signout() {

    }

    @PostMapping(value = "/websocket/token", produces = "application/json")
    @ResponseBody
    String requestWebsocketToken() {

    }

    private boolean isLocalEnv() {
        return environment.getActiveProfiles().length == 0
                && Arrays.equals(environment.getDefaultProfiles(), new String[] {"default"});
    }

    private void doSignup(String email, String name, String password) {

    }

    private ModelAndView redirect(String url) {
        return new ModelAndView("redirect:" + url);
    }

    private ModelAndView prepareModelAndView(String view) {
        ModelAndView mv = new ModelAndView(view);
        addGlobalProperty(mv);
        return mv;
    }

    private ModelAndView prepareModelAndView(String view, Map<String, Object> map) {
        ModelAndView mv = new ModelAndView(view);
        mv.addAllObjects(map);
        addGlobalProperty(mv);
        return mv;
    }

    private void addGlobalProperty(ModelAndView mv) {
        final Long userId = UserContext.getUserId();
        mv.addObject("__userId__", userId);
        mv.addObject("__profile__", userId == null ? null : userService.getUserProfile(userId));
        mv.addObject("__time__", Long.valueOf(System.currentTimeMillis()));
    }

    static Pattern EMAIL = Pattern.compile("^[a-z0-9\\-\\.]+\\@([a-z0-9\\-]+\\.){1,3}[a-z]{2,20}$");

}
