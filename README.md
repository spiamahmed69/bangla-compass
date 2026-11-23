# SP Compass — বাংলা কম্পাস (Android)

বাংলা কম্পাস অ্যাপ। ফোনের আসল কম্পাস সেন্সর (Rotation Vector, না থাকলে
Accelerometer + Magnetometer) দিয়ে উত্তর, দক্ষিণ, পূর্ব, পশ্চিম মাপে।

ডেভেলপার: সিয়াম আহম্মদ

## বিশেষ ফিচার

- দিক সিলেক্ট করো (৮টি দিক) — সিলেক্ট শেষ হলে কম্পাস ফুলস্ক্রিনে আসবে।
- নিচে সহজ বাংলায় দেখাবে: এখন কত ডিগ্রিতে আছো, লক্ষ্য কত ডিগ্রি, আর কত
  ডিগ্রি ডানে বা বামে ঘুরলেই একদম সমান হবে, আর কতটুকু মিলেছে।
- উত্তর / দক্ষিণ / পূর্ব / পশ্চিম — প্রতিটা কতটুকু আছে, সেটাও আলাদা করে দেখায়।
- কোনো ফেক দিক দেখায় না। যে ফোনে কম্পাস সেন্সর নেই, প্রথম খুললেই সেটা
  পরিষ্কার করে বলে দেবে। পরীক্ষার জন্য একটা হাতের স্লাইডার আছে, সেটা
  পরিষ্কারভাবে "সেন্সর নয়" লেখা থাকে।

## Build

```bash
gradle assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
GitHub Actions builds it automatically on every push to `main` and attaches
the APK to a release and as an Actions artifact.

## Tech

Pure Java, Android SDK only — no third-party libraries. Minimum SDK 21,
target SDK 34. Package: com.spcompass.app
