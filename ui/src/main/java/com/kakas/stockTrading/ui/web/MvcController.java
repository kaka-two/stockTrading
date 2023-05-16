package com.kakas.stockTrading.ui.web;

import com.kakas.stockTrading.bean.AuthToken;
import com.kakas.stockTrading.bean.TransferRequestBean;
import com.kakas.stockTrading.client.RestClient;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.dbService.UserProfileServiceImpl;
import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.enums.UserType;
import com.kakas.stockTrading.pojo.UserProfile;
import com.kakas.stockTrading.util.SignatureUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Controller
@Slf4j
public class MvcController {
    @Value("#{tradingConfiguration.hmacKey}")
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
        if(UserContext.getUserId() != null) {
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
        if(UserContext.getUserId() != null) {
            return redirect("/");
        }
        return prepareModelAndView("signin");
    }

    @PostMapping("/signin")
    public ModelAndView signin(@RequestParam("email") String email, @RequestParam("password") String password, HttpServletRequest request, HttpServletResponse response) {
        // check email
        if (email == null || email.isEmpty()) {
            return prepareModelAndView("signin", Map.of("email", email,  "error", "Invalid email or password."));
        }
        email = email.trim().toLowerCase();
        // check password
        if (password == null ||  password.isEmpty()) {
            return prepareModelAndView("signin", Map.of("email", email, "error", "Invalid email or password."));
        }

        UserProfile userProfile = userService.signIn(email, password);
        AuthToken authToken = new AuthToken(userProfile.getUserId(), System.currentTimeMillis() + 1000 * cookieService.getExpiredInSeconds());
        cookieService.setTokenCookie(request, response, authToken);
        return redirect("/");
    }

    @GetMapping("/signout")
    public ModelAndView signout(HttpServletRequest request, HttpServletResponse response) {
        cookieService.deleteTokenCookie(request, response);
        return redirect("/");
    }

    @PostMapping(value = "/websocket/token", produces = "application/json")
    @ResponseBody
    String requestWebsocketToken() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return "\"\"";
        }
        AuthToken authToken = new AuthToken(userId, System.currentTimeMillis() + 60_000);
        String strToken = authToken.toSecureString(hmacKey);
        return "\"" + strToken + "\"";
    }

    private boolean isLocalEnv() {
        return environment.getActiveProfiles().length == 0
                && Arrays.equals(environment.getDefaultProfiles(), new String[] {"default"});
    }

    private UserProfile doSignup(String email, String name, String password) {
        UserProfile profile = userService.signup(email, name, password);
        // 本地开发环境下自动给用户增加资产:
        if (isLocalEnv()) {
            log.warn("auto deposit assets for user {} in local dev env...", profile.getEmail());
            Random random = new Random(profile.getUserId());
            deposit(profile.getUserId(), AssertType.StockA, new BigDecimal(random.nextInt(5_00, 10_00)).movePointLeft(2));
            deposit(profile.getUserId(), AssertType.Money, new BigDecimal(random.nextInt(100000_00, 400000_00)).movePointLeft(2));
        }
        log.info("user signed up: {}", profile);
        return profile;
    }

    private void deposit(Long userId, AssertType asset, BigDecimal amount) {
        var req = new TransferRequestBean();
        req.setTransferId(SignatureUtil.sha256(userId + "/" + asset + "/" + amount.stripTrailingZeros().toPlainString())
                .substring(0, 32));
        req.setAmount(amount);
        req.setAsset(asset);
        req.setFromUserId(UserType.ROOT.getUserTypeId());
        req.setToUserId(userId);
        restClient.post(Map.class, "/internal/transfer", null, req);
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
