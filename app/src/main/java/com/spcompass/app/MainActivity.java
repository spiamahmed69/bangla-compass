package com.spcompass.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    // 8 directions, clockwise from north
    private static final String[] NAMES = {
            "উত্তর", "উত্তর-পূর্ব", "পূর্ব", "দক্ষিণ-পূর্ব",
            "দক্ষিণ", "দক্ষিণ-পশ্চিম", "পশ্চিম", "উত্তর-পশ্চিম"};
    private static final int[] DEGREES = {0, 45, 90, 135, 180, 225, 270, 315};
    private static final String[] SHORT = {"উ", "উ-পূ", "পূ", "দ-পূ", "দ", "দ-প", "প", "উ-প"};

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private final float[] accelReading = new float[3];
    private final float[] magnetReading = new float[3];
    private boolean hasAccel = false;
    private boolean hasMagnet = false;
    private boolean sensorSupported = false;
    private boolean sensorHasData = false;
    private boolean manualMode = false;

    private float heading = 0f; // smoothed, 0-359
    private boolean headingInit = false;
    private int targetDeg = 0;
    private String targetName = "উত্তর";

    private View viewSelect;
    private View viewCompass;
    private CompassView compassView;
    private TextView tvHeading;
    private TextView tvCurrentDir;
    private TextView tvTarget;
    private TextView tvNow;
    private TextView tvGuide;
    private TextView tvStatus;
    private TextView tvSensor;
    private TextView tvNorth;
    private TextView tvSouth;
    private TextView tvEast;
    private TextView tvWest;
    private ProgressBar pbMatch;
    private TextView tvMatch;
    private SeekBar sbManual;
    private TextView tvManualLabel;

    public static String toBn(int n) {
        String s = String.valueOf(n);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') b.append((char) ('০' + (c - '0')));
            else b.append(c);
        }
        return b.toString();
    }

    public static String toBnFloat(float v) {
        String s = String.format(Locale.US, "%.0f", v);
        return toBn(Integer.parseInt(s));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        viewSelect = findViewById(R.id.viewSelect);
        viewCompass = findViewById(R.id.viewCompass);
        compassView = findViewById(R.id.compassView);
        tvHeading = findViewById(R.id.tvHeading);
        tvCurrentDir = findViewById(R.id.tvCurrentDir);
        tvTarget = findViewById(R.id.tvTarget);
        tvNow = findViewById(R.id.tvNow);
        tvGuide = findViewById(R.id.tvGuide);
        tvStatus = findViewById(R.id.tvStatus);
        tvSensor = findViewById(R.id.tvSensor);
        tvNorth = findViewById(R.id.tvNorth);
        tvSouth = findViewById(R.id.tvSouth);
        tvEast = findViewById(R.id.tvEast);
        tvWest = findViewById(R.id.tvWest);
        pbMatch = findViewById(R.id.pbMatch);
        tvMatch = findViewById(R.id.tvMatch);
        sbManual = findViewById(R.id.sbManual);
        tvManualLabel = findViewById(R.id.tvManualLabel);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorSupported = rotationVectorSensor != null
                || (accelerometer != null && magnetometer != null);

        // Direction buttons on the select screen
        int[] btnIds = {R.id.btnN, R.id.btnNE, R.id.btnE, R.id.btnSE,
                R.id.btnS, R.id.btnSW, R.id.btnW, R.id.btnNW};
        for (int i = 0; i < btnIds.length; i++) {
            final int idx = i;
            Button b = findViewById(btnIds[i]);
            b.setText(NAMES[i] + "\n" + toBn(DEGREES[i]) + "°");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    targetDeg = DEGREES[idx];
                    targetName = NAMES[idx];
                    showCompass();
                }
            });
        }

        findViewById(R.id.btnChangeDir).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewCompass.setVisibility(View.GONE);
                viewSelect.setVisibility(View.VISIBLE);
            }
        });

        sbManual.setMax(359);
        sbManual.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    manualMode = true;
                    setHeading((float) progress);
                    tvManualLabel.setText("পরীক্ষার জন্য হাতে ঘোরানো: " + toBn(progress) + "°  (এটা সেন্সর নয়)");
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateGuide();
        if (!sensorSupported) {
            tvSensor.setText("এই ফোনে কম্পাস সেন্সর নেই — এই ফোন কম্পাস সাপোর্ট করে না। নিচের স্লাইডার দিয়ে পরীক্ষা করে দেখতে পারবে, তবে এটা আসল দিক নয়।");
            Toast.makeText(this, "এই ফোনে কম্পাস সেন্সর নেই", Toast.LENGTH_LONG).show();
        } else {
            tvSensor.setText("সেন্সরের জন্য অপেক্ষা করছি… ফোনটি হাতে ধরে আস্তে ঘোরাও।");
        }
    }

    private void showCompass() {
        viewSelect.setVisibility(View.GONE);
        viewCompass.setVisibility(View.VISIBLE);
        manualMode = false;
        updateGuide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (manualMode) return;
        float azimuth;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rot = new float[9];
            SensorManager.getRotationMatrixFromVector(rot, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(rot, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelReading, 0, 3);
            hasAccel = true;
            if (!hasMagnet) return;
            azimuth = computeAzimuthFromAccelMagnet();
            if (Float.isNaN(azimuth)) return;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetReading, 0, 3);
            hasMagnet = true;
            if (!hasAccel) return;
            azimuth = computeAzimuthFromAccelMagnet();
            if (Float.isNaN(azimuth)) return;
        } else {
            return;
        }
        if (azimuth < 0) azimuth += 360f;
        sensorHasData = true;
        setHeading(azimuth);
        tvSensor.setText("সেন্সর চলছে — আসল দিক দেখাচ্ছে।");
    }

    private float computeAzimuthFromAccelMagnet() {
        float[] r = new float[9];
        float[] i = new float[9];
        if (!SensorManager.getRotationMatrix(r, i, accelReading, magnetReading)) return Float.NaN;
        float[] o = new float[3];
        SensorManager.getOrientation(r, o);
        return (float) Math.toDegrees(o[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            tvSensor.setText("সেন্সর ঠিকমতো কাজ করছে না — ফোনটি ৮-এর মতো ঘুরিয়ে ক্যালিব্রেট করো।");
        }
    }

    private void setHeading(float newHeading) {
        if (!headingInit) {
            heading = newHeading;
            headingInit = true;
        } else {
            // Circular smoothing (shortest path)
            float diff = newHeading - heading;
            while (diff > 180f) diff -= 360f;
            while (diff < -180f) diff += 360f;
            heading += diff * 0.18f;
            if (heading < 0) heading += 360f;
            if (heading >= 360f) heading -= 360f;
        }
        compassView.setHeading(heading);
        updateGuide();
    }

    private String dirName(float h) {
        int idx = Math.round(h / 45f) % 8;
        return NAMES[idx];
    }

    private void updateGuide() {
        int h = Math.round(heading) % 360;
        tvHeading.setText(toBn(h) + "°");
        tvCurrentDir.setText("এখন: " + dirName(heading));
        tvTarget.setText("লক্ষ্য: " + targetName + " (" + toBn(targetDeg) + "°)");
        tvNow.setText("এখন তুমি " + toBn(h) + "°-তে আছো — " + dirName(heading) + " দিকে");

        // Deviation from target, -180..180
        float diff = targetDeg - heading;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        int absDiff = Math.round(Math.abs(diff));
        int match = Math.max(0, 100 - (int) Math.round(absDiff / 180.0 * 100));
        pbMatch.setProgress(match);
        tvMatch.setText("মিলেছে: " + toBn(match) + "%");

        if (absDiff <= 3) {
            tvGuide.setText("একদম সমান হয়েছে! তুমি ঠিক " + targetName + " দিকে আছো।");
            tvStatus.setText("একদম সমান");
        } else if (absDiff <= 10) {
            String dir = diff > 0 ? "ডানে" : "বামে";
            tvGuide.setText("প্রায় হয়েছে! আর মাত্র " + toBn(absDiff) + "° " + dir + "দিকে ঘোরো, তাহলেই " + targetName + " সমান হবে।");
            tvStatus.setText("প্রায় হয়েছে");
        } else if (absDiff >= 150) {
            String dir = diff > 0 ? "ডানে" : "বামে";
            tvGuide.setText("তুমি উল্টোদিকে আছো। " + targetName + " পেতে " + toBn(absDiff) + "° " + dir + "দিকে ঘুরতে হবে।");
            tvStatus.setText("উল্টোদিকে আছো");
        } else {
            String dir = diff > 0 ? "ডানে" : "বামে";
            tvGuide.setText("আর " + toBn(absDiff) + "° " + dir + "দিকে ঘোরো, তাহলেই " + targetName + " (" + toBn(targetDeg) + "°) সমান হবে।");
            tvStatus.setText("আর একটু ঘোরো");
        }

        // How much north / south / east / west the current heading contains
        double rad = Math.toRadians(heading);
        int north = (int) Math.round(Math.max(0, Math.cos(rad)) * 100);
        int south = (int) Math.round(Math.max(0, -Math.cos(rad)) * 100);
        int east = (int) Math.round(Math.max(0, Math.sin(rad)) * 100);
        int west = (int) Math.round(Math.max(0, -Math.sin(rad)) * 100);
        tvNorth.setText("উত্তর\n" + toBn(north) + "%");
        tvSouth.setText("দক্ষিণ\n" + toBn(south) + "%");
        tvEast.setText("পূর্ব\n" + toBn(east) + "%");
        tvWest.setText("পশ্চিম\n" + toBn(west) + "%");

        if (!sensorSupported) {
            tvSensor.setText("এই ফোনে কম্পাস সেন্সর নেই — এই ফোন কম্পাস সাপোর্ট করে না। নিচের স্লাইডার দিয়ে পরীক্ষা করে দেখতে পারবে, তবে এটা আসল দিক নয়।");
        } else if (!sensorHasData && !manualMode) {
            tvSensor.setText("সেন্সরের জন্য অপেক্ষা করছি… ফোনটি হাতে ধরে আস্তে ঘোরাও।");
        }
    }
}
