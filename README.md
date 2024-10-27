<div align="center"><picture>
  <img alt="" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png">
</picture><br>
<h1 align="center">IR for Tasker</h1>

An IR signal transmission plugin for Tasker

</div>

## :satellite: Transmission Method

The app can transmit IR signals through the built-in ir blaster
or [DIY IR blaster](https://www.instructables.com/How-to-Make-an-IR-Blaster/)
signal as audio pulses through the 3.5mm jack.

> [!IMPORTANT]
> Check below instructions before using this app.


<details><summary>

## :sound: Instructions for Audio Transmission Method

</summary>

## :warning: Disclaimer

> [!IMPORTANT]
> Usage of this app is at your own risk. I'm not responsible for any loss or damage associated with
> the use of this app.

- App is in the early development stage, and its functionality is not guaranteed.
- Transmitting as an audio pulse is not reliable due to varying device-dependent audio processing
  methods.
- The maximum working range during testing was found to be 1.5 meters.

## :safety_vest: Safety Recommendations

> [!WARNING]
> It is important to exercise caution while using this app as there is a risk of damaging the audio
> circuits due to mishandling.

- The audio pulses generated from IR data are audible, plays at **full volume** and may cause ear
  discomfort, so it is advised not to use this app with headphones connected (not event BT
  headphones).
- LED type and quality can result in drawing different amounts of current, so there is a chance for
  damaging the circuits.
- Improper connection may result in damaging the device.

</details>

## :white_check_mark: Code Verification and Troubleshooting

To prevent possible crashes and provide error reports with Tasker, the app verifies the input every
time. It is possible for a valid IR code to be rejected as invalid due to a non-tested scenario,
this occurrence is rare.
> [!NOTE]  
> If you found a code is not working with app, try
> with [Termux-API](https://wiki.termux.com/wiki/Termux-infrared-transmit), if the code is in hex
>
format,use [this task](https://taskernet.com/shares/?user=AS35m8mVC%2FNlWH31JCTnGHpKVeZk1osEp8V1pFxCq1Ls28Un1RXCw9ZNWWvmpxOebt4WIYFeiZhZKHc%3D&id=Task%3AIR+-+Pronto+Hex+To+Raw+Pulses).

## :keyboard: Input Options

Works with:

1. [ProntoHex code](https://www.etcwiki.org/wiki/Pronto_Infrared_Format)

eg: `0000 006D 0000 0008 0060 0040 0040 0020 0020 0040 0020 0040 0020 0040 0020 0040 0020 0020 0020 0D7A`

2. Raw Pulses

Carrier frequency in Hertz followed by the alternating on/off pattern in microseconds.

eg:  `38028, 2526, 1684, 1684, 842, 842, 1684, 842, 1684, 842, 1684, 842, 1684, 842, 842, 842, 90789`

## :books: References

1. [RemoteCentral.com - ProntoHex structure](https://www.remotecentral.com/features/irdisp1.htm)

2. [ProntoDroid - Audio pulse method](https://github.com/g-r-a-v-i-t-y-w-a-v-e/ProntoDroid)

3. [Remote control working](https://www.reddit.com/r/homeautomation/comments/kqaggm/how_does_the_remote_control_work_explained/)
