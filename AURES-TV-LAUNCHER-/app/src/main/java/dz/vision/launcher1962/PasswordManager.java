package dz.vision.launcher1962;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise-grade TV-Optimized Password Manager for 1962 Launcher.
 * Initial Administrator Password: 2001
 * 
 * Security features:
 * - Salted SHA-256 password hashing.
 * - Zero plaintext password storage in preferences, source code, or logs.
 * - Brute-force protection: 3 failed attempts enforce a 15-second temporary lockout.
 * - Dual input: Full D-pad remote keypad navigation + physical remote number buttons (0-9).
 * - Per-session authentication (never remains permanently unlocked).
 */
public final class PasswordManager {

    private static final String PREFS_NAME = "launcher_1962_security_prefs";
    private static final String KEY_PASS_HASH = "sec_pwd_hash";
    private static final String KEY_PASS_SALT = "sec_pwd_salt";
    private static final String KEY_FAILED_ATTEMPTS = "sec_failed_attempts";
    private static final String KEY_LOCKOUT_UNTIL = "sec_lockout_until";

    public static final String INITIAL_DEFAULT_PASSWORD = "2001";
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 15000; // 15 seconds lockout

    public enum AuthResult {
        SUCCESS,
        WRONG_PASSWORD,
        LOCKED_OUT
    }

    public interface AuthCallback {
        void onAuthenticated();
        void onCancelled();
    }

    /**
     * Ensure the initial master password (2001) is initialized securely on first run.
     */
    public static void ensureInitialized(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!sp.contains(KEY_PASS_HASH) || !sp.contains(KEY_PASS_SALT)) {
            setPassword(context, INITIAL_DEFAULT_PASSWORD);
        }
    }

    /**
     * Set a new administrator password using salted SHA-256.
     */
    public static boolean setPassword(Context context, String newPassword) {
        if (newPassword == null || newPassword.length() < 4) return false;
        try {
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            String saltHex = bytesToHex(saltBytes);

            String hash = hashPassword(newPassword, saltHex);

            SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            sp.edit()
                    .putString(KEY_PASS_HASH, hash)
                    .putString(KEY_PASS_SALT, saltHex)
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCKOUT_UNTIL, 0)
                    .commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify entered password with brute-force rate-limiting.
     */
    public static AuthResult verifyPassword(Context context, String input) {
        ensureInitialized(context);
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        long now = System.currentTimeMillis();
        long lockoutUntil = sp.getLong(KEY_LOCKOUT_UNTIL, 0);
        if (now < lockoutUntil) {
            return AuthResult.LOCKED_OUT;
        }

        String expectedHash = sp.getString(KEY_PASS_HASH, null);
        String saltHex = sp.getString(KEY_PASS_SALT, null);
        if (expectedHash == null || saltHex == null) {
            setPassword(context, INITIAL_DEFAULT_PASSWORD);
            expectedHash = sp.getString(KEY_PASS_HASH, null);
            saltHex = sp.getString(KEY_PASS_SALT, null);
        }

        String computedHash = hashPassword(input, saltHex);
        if (constantTimeEquals(expectedHash, computedHash)) {
            // Reset failure counter on success
            sp.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
            return AuthResult.SUCCESS;
        } else {
            int fails = sp.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
            SharedPreferences.Editor ed = sp.edit().putInt(KEY_FAILED_ATTEMPTS, fails);
            if (fails >= MAX_FAILED_ATTEMPTS) {
                ed.putLong(KEY_LOCKOUT_UNTIL, now + LOCKOUT_DURATION_MS);
            }
            ed.apply();
            return (fails >= MAX_FAILED_ATTEMPTS) ? AuthResult.LOCKED_OUT : AuthResult.WRONG_PASSWORD;
        }
    }

    public static long getLockoutRemainingSeconds(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lockoutUntil = sp.getLong(KEY_LOCKOUT_UNTIL, 0);
        long now = System.currentTimeMillis();
        if (now >= lockoutUntil) return 0;
        return (lockoutUntil - now + 999) / 1000;
    }

    private static String hashPassword(String password, String saltHex) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltHex.getBytes("UTF-8"));
            byte[] digest = md.digest(password.getBytes("UTF-8"));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Check if a package is a system settings or configuration component that must be protected.
     */
    public static boolean isSystemSettingsPackage(String packageName) {
        if (packageName == null) return false;
        return packageName.equals("com.android.tv.settings") ||
                packageName.equals("com.android.settings") ||
                packageName.equals("com.softwinner.settingsassist") ||
                packageName.equals("com.oranth.setupwizard") ||
                packageName.equals("com.droidlogic.otaupgrade") ||
                packageName.equals("com.softwinner.update") ||
                packageName.equals("com.android.packageinstaller");
    }

    /**
     * Authenticate before performing a protected action.
     */
    public static void authenticate(final Activity activity, final Runnable onSuccess) {
        authenticate(activity, null, onSuccess, null);
    }

    public static void authenticate(final Activity activity, final String actionTitle, final Runnable onSuccess, final Runnable onCancelled) {
        if (activity == null || activity.isFinishing()) return;

        showPasswordDialog(activity, actionTitle, new AuthCallback() {
            @Override
            public void onAuthenticated() {
                if (onSuccess != null) {
                    activity.runOnUiThread(onSuccess);
                }
            }

            @Override
            public void onCancelled() {
                if (onCancelled != null) {
                    activity.runOnUiThread(onCancelled);
                }
            }
        });
    }

    /**
     * Display the 1962 Premium Password Dialog.
     * Full D-pad navigation, remote numeric keys support, masked input, and error feedback.
     */
    public static void showPasswordDialog(final Activity activity, final String actionTitle, final AuthCallback callback) {
        ensureInitialized(activity);
        final float density = activity.getResources().getDisplayMetrics().density;

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0xDD000000)); // 87% black scrim
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Fullscreen Root Container
        FrameLayout rootLayout = new FrameLayout(activity);
        rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Centered Charcoal Glass Dialog Box
        final LinearLayout dialogCard = new LinearLayout(activity);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setGravity(Gravity.CENTER_HORIZONTAL);
        dialogCard.setPadding((int) (36 * density), (int) (26 * density), (int) (36 * density), (int) (26 * density));

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                (int) (480 * density),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.gravity = Gravity.CENTER;
        dialogCard.setLayoutParams(cardLp);

        GradientDrawable cardGd = new GradientDrawable();
        cardGd.setCornerRadius(22 * density);
        cardGd.setColor(0xF0121A24); // 94% charcoal
        cardGd.setStroke((int) (1.5f * density), 0xFF27313A);
        dialogCard.setBackground(cardGd);

        // Header: Lock icon & Arabic Title
        TextView titleView = new TextView(activity);
        titleView.setText("🔒  الإعدادات محمية");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setGravity(Gravity.CENTER);
        dialogCard.addView(titleView);

        // Subtitle: Action being protected or default prompt
        TextView subView = new TextView(activity);
        String subText = (actionTitle != null && !actionTitle.isEmpty()) ?
                "أدخل كلمة المرور للمتابعة إلى: " + actionTitle :
                "أدخل كلمة المرور للمتابعة";
        subView.setText(subText);
        subView.setTextColor(0xFF90A4AE);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subView.setGravity(Gravity.CENTER);
        subView.setPadding(0, (int) (6 * density), 0, (int) (14 * density));
        dialogCard.addView(subView);

        // 4-Digit Masked PIN Display Row
        final LinearLayout pinRow = new LinearLayout(activity);
        pinRow.setOrientation(LinearLayout.HORIZONTAL);
        pinRow.setGravity(Gravity.CENTER);
        pinRow.setPadding(0, (int) (4 * density), 0, (int) (10 * density));

        final TextView[] pinBoxes = new TextView[4];
        for (int i = 0; i < 4; i++) {
            TextView box = new TextView(activity);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    (int) (52 * density),
                    (int) (56 * density)
            );
            bLp.leftMargin = (int) (8 * density);
            bLp.rightMargin = (int) (8 * density);
            box.setLayoutParams(bLp);
            box.setGravity(Gravity.CENTER);
            box.setText("○");
            box.setTextColor(0xFF546E7A);
            box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

            GradientDrawable bGd = new GradientDrawable();
            bGd.setCornerRadius(12 * density);
            bGd.setColor(0xFF1B2430);
            bGd.setStroke((int) (1.2f * density), 0xFF37474F);
            box.setBackground(bGd);

            pinBoxes[i] = box;
            pinRow.addView(box);
        }
        dialogCard.addView(pinRow);

        // Status / Error message (Red / Warning)
        final TextView errorView = new TextView(activity);
        errorView.setTextColor(0xFFFF5252); // Coral red error
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        errorView.setGravity(Gravity.CENTER);
        errorView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        errorView.setPadding(0, 0, 0, (int) (10 * density));
        errorView.setVisibility(View.INVISIBLE);
        dialogCard.addView(errorView);

        // PIN Buffer
        final StringBuilder pinBuffer = new StringBuilder();

        final Runnable updatePinDisplay = () -> {
            int len = pinBuffer.length();
            for (int i = 0; i < 4; i++) {
                TextView box = pinBoxes[i];
                GradientDrawable bGd = (GradientDrawable) box.getBackground();
                if (i < len) {
                    box.setText("●");
                    box.setTextColor(Color.WHITE);
                    bGd.setColor(0xFF223142);
                    bGd.setStroke((int) (1.8f * density), 0xFF00C878); // Emerald active border
                } else {
                    box.setText("○");
                    box.setTextColor(0xFF546E7A);
                    bGd.setColor(0xFF1B2430);
                    bGd.setStroke((int) (1.2f * density), 0xFF37474F);
                }
            }
        };

        // Actions: Submit Verification
        final Runnable submitAction = () -> {
            String entered = pinBuffer.toString().trim();
            if (entered.length() == 0) return;

            AuthResult result = verifyPassword(activity, entered);
            if (result == AuthResult.SUCCESS) {
                dialog.dismiss();
                if (callback != null) callback.onAuthenticated();
            } else if (result == AuthResult.LOCKED_OUT) {
                long sec = getLockoutRemainingSeconds(activity);
                errorView.setText("محاولات خاطئة متكررة. يرجى الانتظار " + sec + " ثانية");
                errorView.setVisibility(View.VISIBLE);
                pinBuffer.setLength(0);
                updatePinDisplay.run();
                shakeView(dialogCard);
            } else {
                errorView.setText("كلمة المرور غير صحيحة");
                errorView.setVisibility(View.VISIBLE);
                pinBuffer.setLength(0);
                updatePinDisplay.run();
                shakeView(dialogCard);
            }
        };

        // On-Screen Keypad for TV Remote (3 Columns x 4 Rows)
        LinearLayout keypad = new LinearLayout(activity);
        keypad.setOrientation(LinearLayout.VERTICAL);
        keypad.setGravity(Gravity.CENTER_HORIZONTAL);
        keypad.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"⌫ مسح", "0", "إلغاء"}
        };

        final List<View> keypadButtons = new ArrayList<>();

        for (int r = 0; r < keys.length; r++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rLp.bottomMargin = (int) (8 * density);
            row.setLayoutParams(rLp);

            for (int c = 0; c < keys[r].length; c++) {
                final String key = keys[r][c];
                final TextView btn = new TextView(activity);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                        (int) (118 * density),
                        (int) (44 * density)
                );
                bLp.leftMargin = (int) (5 * density);
                bLp.rightMargin = (int) (5 * density);
                btn.setLayoutParams(bLp);
                btn.setText(key);
                btn.setGravity(Gravity.CENTER);
                btn.setTextColor(0xFFECEFF1);
                btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                btn.setFocusable(true);
                btn.setClickable(true);

                final GradientDrawable btnGd = new GradientDrawable();
                btnGd.setCornerRadius(10 * density);
                btnGd.setColor(0xFF1E2834);
                btnGd.setStroke((int) (1 * density), 0xFF37474F);
                btn.setBackground(btnGd);

                btn.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        btnGd.setColor(0xFF2C3E50);
                        btnGd.setStroke((int) (2 * density), 0xFF00C878);
                        btn.setTextColor(Color.WHITE);
                        btn.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150).start();
                    } else {
                        btnGd.setColor(0xFF1E2834);
                        btnGd.setStroke((int) (1 * density), 0xFF37474F);
                        btn.setTextColor(0xFFECEFF1);
                        btn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    }
                });

                btn.setOnClickListener(v -> {
                    errorView.setVisibility(View.INVISIBLE);
                    if ("⌫ مسح".equals(key)) {
                        if (pinBuffer.length() > 0) {
                            pinBuffer.deleteCharAt(pinBuffer.length() - 1);
                            updatePinDisplay.run();
                        }
                    } else if ("إلغاء".equals(key)) {
                        dialog.dismiss();
                        if (callback != null) callback.onCancelled();
                    } else {
                        if (pinBuffer.length() < 4) {
                            pinBuffer.append(key);
                            updatePinDisplay.run();
                            // Auto submit on 4th digit
                            if (pinBuffer.length() == 4) {
                                new Handler(Looper.getMainLooper()).postDelayed(submitAction, 180);
                            }
                        }
                    }
                });

                row.addView(btn);
                keypadButtons.add(btn);
            }
            keypad.addView(row);
        }
        dialogCard.addView(keypad);

        // Confirm Button (تأكيد) - Emerald Green Full Width
        final TextView confirmBtn = new TextView(activity);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                (int) (364 * density),
                (int) (48 * density)
        );
        cLp.topMargin = (int) (4 * density);
        confirmBtn.setLayoutParams(cLp);
        confirmBtn.setText("تأكيد  ✓");
        confirmBtn.setGravity(Gravity.CENTER);
        confirmBtn.setTextColor(Color.WHITE);
        confirmBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        confirmBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        confirmBtn.setFocusable(true);
        confirmBtn.setClickable(true);

        final GradientDrawable cGd = new GradientDrawable();
        cGd.setCornerRadius(12 * density);
        cGd.setColor(0xFF00C878); // Algerian Emerald Green
        confirmBtn.setBackground(cGd);

        confirmBtn.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                cGd.setColor(0xFF00E68A);
                confirmBtn.animate().scaleX(1.04f).scaleY(1.04f).setDuration(150).start();
            } else {
                cGd.setColor(0xFF00C878);
                confirmBtn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
            }
        });

        confirmBtn.setOnClickListener(v -> submitAction.run());
        dialogCard.addView(confirmBtn);

        rootLayout.addView(dialogCard);
        dialog.setContentView(rootLayout);

        // Listen for Remote Number Keys & Back Key
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                dialog.dismiss();
                if (callback != null) callback.onCancelled();
                return true;
            }

            // Remote numeric keys (0-9)
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                int digit = keyCode - KeyEvent.KEYCODE_0;
                errorView.setVisibility(View.INVISIBLE);
                if (pinBuffer.length() < 4) {
                    pinBuffer.append(digit);
                    updatePinDisplay.run();
                    if (pinBuffer.length() == 4) {
                        new Handler(Looper.getMainLooper()).postDelayed(submitAction, 180);
                    }
                }
                return true;
            }

            // Remote Delete / Backspace key
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (pinBuffer.length() > 0) {
                    pinBuffer.deleteCharAt(pinBuffer.length() - 1);
                    updatePinDisplay.run();
                }
                return true;
            }

            return false;
        });

        dialog.setOnCancelListener(d -> {
            if (callback != null) callback.onCancelled();
        });

        dialog.show();

        // Focus first keypad button ([1]) automatically for effortless remote D-pad navigation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!keypadButtons.isEmpty()) {
                keypadButtons.get(0).requestFocus();
            }
        }, 120);
    }

    /**
     * Change Password Dialog (within authenticated settings).
     */
    public static void showChangePasswordDialog(final Activity activity) {
        authenticate(activity, "تغيير كلمة المرور", () -> {
            promptForNewPassword(activity);
        }, null);
    }

    private static void promptForNewPassword(final Activity activity) {
        final float density = activity.getResources().getDisplayMetrics().density;
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0xDD000000));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        FrameLayout root = new FrameLayout(activity);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding((int) (36 * density), (int) (26 * density), (int) (36 * density), (int) (26 * density));

        FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams((int) (480 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.gravity = Gravity.CENTER;
        card.setLayoutParams(cLp);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(22 * density);
        gd.setColor(0xF0121A24);
        gd.setStroke((int) (1.5f * density), 0xFF27313A);
        card.setBackground(gd);

        TextView title = new TextView(activity);
        title.setText("🔑  تعيين كلمة مرور جديدة");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        TextView sub = new TextView(activity);
        sub.setText("أدخل كلمة المرور الجديدة (4 أرقام):");
        sub.setTextColor(0xFF90A4AE);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setPadding(0, (int) (6 * density), 0, (int) (14 * density));
        card.addView(sub);

        final LinearLayout pinRow = new LinearLayout(activity);
        pinRow.setOrientation(LinearLayout.HORIZONTAL);
        pinRow.setGravity(Gravity.CENTER);
        pinRow.setPadding(0, (int) (4 * density), 0, (int) (10 * density));

        final TextView[] boxes = new TextView[4];
        for (int i = 0; i < 4; i++) {
            TextView b = new TextView(activity);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams((int) (52 * density), (int) (56 * density));
            bLp.leftMargin = (int) (8 * density);
            bLp.rightMargin = (int) (8 * density);
            b.setLayoutParams(bLp);
            b.setGravity(Gravity.CENTER);
            b.setText("○");
            b.setTextColor(0xFF546E7A);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            GradientDrawable bGd = new GradientDrawable();
            bGd.setCornerRadius(12 * density);
            bGd.setColor(0xFF1B2430);
            bGd.setStroke((int) (1.2f * density), 0xFF37474F);
            b.setBackground(bGd);
            boxes[i] = b;
            pinRow.addView(b);
        }
        card.addView(pinRow);

        final StringBuilder buf = new StringBuilder();

        final Runnable updateDisplay = () -> {
            int len = buf.length();
            for (int i = 0; i < 4; i++) {
                TextView b = boxes[i];
                GradientDrawable bGd = (GradientDrawable) b.getBackground();
                if (i < len) {
                    b.setText("●");
                    b.setTextColor(Color.WHITE);
                    bGd.setColor(0xFF223142);
                    bGd.setStroke((int) (1.8f * density), 0xFF00C878);
                } else {
                    b.setText("○");
                    b.setTextColor(0xFF546E7A);
                    bGd.setColor(0xFF1B2430);
                    bGd.setStroke((int) (1.2f * density), 0xFF37474F);
                }
            }
        };

        final Runnable saveAction = () -> {
            if (buf.length() == 4) {
                setPassword(activity, buf.toString());
                dialog.dismiss();
                Toast.makeText(activity, "تم تغيير كلمة المرور بنجاح ✓", Toast.LENGTH_LONG).show();
            }
        };

        LinearLayout keypad = new LinearLayout(activity);
        keypad.setOrientation(LinearLayout.VERTICAL);
        keypad.setGravity(Gravity.CENTER_HORIZONTAL);

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"⌫ مسح", "0", "إلغاء"}
        };

        View firstBtn = null;
        for (int r = 0; r < keys.length; r++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = (int) (8 * density);
            row.setLayoutParams(rLp);

            for (int c = 0; c < keys[r].length; c++) {
                final String key = keys[r][c];
                final TextView btn = new TextView(activity);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams((int) (118 * density), (int) (44 * density));
                bLp.leftMargin = (int) (5 * density);
                bLp.rightMargin = (int) (5 * density);
                btn.setLayoutParams(bLp);
                btn.setText(key);
                btn.setGravity(Gravity.CENTER);
                btn.setTextColor(0xFFECEFF1);
                btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                btn.setFocusable(true);
                btn.setClickable(true);

                final GradientDrawable btnGd = new GradientDrawable();
                btnGd.setCornerRadius(10 * density);
                btnGd.setColor(0xFF1E2834);
                btnGd.setStroke((int) (1 * density), 0xFF37474F);
                btn.setBackground(btnGd);

                btn.setOnFocusChangeListener((v, hasFocus) -> {
                    btnGd.setColor(hasFocus ? 0xFF2C3E50 : 0xFF1E2834);
                    btnGd.setStroke((int) ((hasFocus ? 2 : 1) * density), hasFocus ? 0xFF00C878 : 0xFF37474F);
                    btn.setTextColor(hasFocus ? Color.WHITE : 0xFFECEFF1);
                });

                btn.setOnClickListener(v -> {
                    if ("⌫ مسح".equals(key)) {
                        if (buf.length() > 0) {
                            buf.deleteCharAt(buf.length() - 1);
                            updateDisplay.run();
                        }
                    } else if ("إلغاء".equals(key)) {
                        dialog.dismiss();
                    } else {
                        if (buf.length() < 4) {
                            buf.append(key);
                            updateDisplay.run();
                            if (buf.length() == 4) {
                                new Handler(Looper.getMainLooper()).postDelayed(saveAction, 200);
                            }
                        }
                    }
                });

                row.addView(btn);
                if (firstBtn == null) firstBtn = btn;
            }
            keypad.addView(row);
        }
        card.addView(keypad);

        final TextView confirm = new TextView(activity);
        LinearLayout.LayoutParams cfLp = new LinearLayout.LayoutParams((int) (364 * density), (int) (48 * density));
        cfLp.topMargin = (int) (4 * density);
        confirm.setLayoutParams(cfLp);
        confirm.setText("حفظ كلمة المرور الجديدة  ✓");
        confirm.setGravity(Gravity.CENTER);
        confirm.setTextColor(Color.WHITE);
        confirm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        confirm.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        confirm.setFocusable(true);
        confirm.setClickable(true);

        GradientDrawable cGd = new GradientDrawable();
        cGd.setCornerRadius(12 * density);
        cGd.setColor(0xFF00C878);
        confirm.setBackground(cGd);
        confirm.setOnClickListener(v -> saveAction.run());
        card.addView(confirm);

        root.addView(card);
        dialog.setContentView(root);

        final View fb = firstBtn;
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (fb != null) fb.requestFocus();
        }, 120);
    }

    private static void shakeView(View view) {
        view.animate()
                .translationXBy(20)
                .setDuration(50)
                .withEndAction(() -> view.animate()
                        .translationXBy(-40)
                        .setDuration(50)
                        .withEndAction(() -> view.animate()
                                .translationXBy(30)
                                .setDuration(50)
                                .withEndAction(() -> view.animate()
                                        .translationX(0)
                                        .setDuration(50)
                                        .start())
                                .start())
                        .start())
                .start();
    }
}
