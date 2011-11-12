Remains  {
	var s;

	var ig, xg, og;
	var in_b, freq_b, amp_b, out_b, out2_b;

	var <>in_s, inputchannel;
	var <>out1_s, <>out2_s, outputchannel;
	var freq_s, amp_s;
	
	var vosc_bufs, delay_bufs;
	var echo_s;
	var sines, whichsine, attacktime;
	
	var newosc;
	
	var amp_thresh, amp_t_thresh, wasinrange;
	var targetpitch, lastnote, lasthighamp;
	
	var dur;
	var whichecho;
	
	var <>delta_t;
	
	var pitch_amp_logic;
	
	var out_limiter_patch_1, out_limiter_patch_2;
	
	
	/////////////// gui
	var remains_ctl_win;
	var remains_logic_win;
	
	var pregain_label, thresh_label, postgain_label;
	var pregain_numbox, thresh_numbox, postgain_numbox;
	var pregain_slider, thresh_slider, postgain_slider;
	
	var lim_pregain_db, lim_thresh_db, lim_postgain_db;
	
	var gain_spec, thresh_spec;

	var pollperiod_label, timethreshold_label;
	var pollperiod_numbox, timethreshold_numbox;
	
	var signal_label, signal_led;
	var ampthreshold_label, ampthreshold_numbox;
	
	var storedefaults_button;
	var defaults_file;
		
	*new {
		arg server, inputchannel, outputchannel;
		^super.new.init(server, inputchannel, outputchannel);
	}

//////////////////////// init
	init { arg server, inchannel, outchannel;
	
		inputchannel = inchannel;
		outputchannel = outchannel;
		
		s = server;	
		
		Routine {
	
		
		//-------------------------------------------------------------- synthdefs
		SynthDef.new(\vorg_1shot, 
		{	arg out=0, timbre1_0, timbre1_1, timbre2_0, timbre2_1, timbre3_0, timbre3_1,
				freq=440, gate=1, level=0.1, attack=0.1, release=0.1, dur=10,
				filter_level=0.0, noise_level = 0.0, sub_level=1.0,
				numbufs=4, basebuf=0,
				panfactor1 = 0.0, panfactor2 = 0.0;
			var timbre1, timbre2, timbre3,
				sin, saw, ring, saws,
				amp, saw_amp,
				env, noise, filter,
				freq_comp;
				
			timbre1 = Line.kr(timbre1_0, timbre1_1, attack + dur + release);
			timbre2 = Line.kr(timbre2_0, timbre2_1, attack + dur + release);
			timbre3 = Line.kr(timbre3_0, timbre3_1, attack + dur + release);
			env = Env.linen(attack, dur, release, 1, 'lin');
			amp = EnvGen.ar(env, gate, 1, doneAction:2);
			saw_amp = EnvGen.ar(env, gate, timeScale:2.0);
			//freq_comp = AmpCompA.kr(freq);
			freq_comp = freq;
			saw = Pan2.ar(SyncSaw.ar(freq_comp, freq * (2 ** (3 * timbre2))), panfactor2);
			sin = Mix.new([Pan2.ar(VOsc.ar((timbre1 * numbufs) + basebuf, freq_comp), panfactor1), Pan2.ar(FSinOsc.ar(freq * 0.5) * sub_level, -1 * panfactor1)]);
			ring = Pan2.ar(Mix.ar([sin * saw]), panfactor2 * -1);
			saws = Mix.new([saw * timbre3, ring * timbre3]) * saw_amp;
			filter = LPF.ar(Mix.new([sin, saws]), freq + (filter_level * (9000-freq)));
			Out.ar(out, filter * amp * level);
		}).send(s);
		
	
		
		SynthDef.new(\shapedelay_env, {
			arg in=0, out=1, delbuf=0, shapebuf=0,
				delaytime=0, level=1, freq=440,
				attack=1, release=1, gate=1, 
				lpf_freq=2000, hpf_freq=80;
			var sig, amp, env;
			sig = HPF.ar(LPF.ar(BufDelayN.ar(delbuf, In.ar(in, 1), delaytime), lpf_freq), hpf_freq);
			sig = Shaper.ar(shapebuf, sig);
			sig = RLPF.ar(sig, Lag.kr(freq, delaytime), 0.4); 
			env = Env.asr(attack, 1, release);
			amp = EnvGen.ar(env, gate, 1); // sustain forever
			Out.ar(out, sig*amp*level);
		}).send(s);
		
		SynthDef.new("in_adc", {arg in=0, out=0, amp=1;
			var sig;
			sig = SoundIn.ar(in);
			Out.ar(out, sig * amp);
		}).send(s);
		
		SynthDef.new("patch_mono", {arg in=0, out=0, amp=1.0, offset=0;
			var input;
			input = In.ar(in, 1);
			Out.ar(out, (input * amp) + offset);
		}).send(s);
		
		SynthDef.new("patch_pan", {arg in=0, out=0, amp=1.0, pan=0;
			var input;
			input = In.ar(in, 1);
			Out.ar(out, Pan2.ar(input*amp, pan));
		}).send(s);
		
		SynthDef.new(\inputamp, {arg in=0, out=0, mul=1.0, add=0.0, attack=0.01, release=0.01;
			var sig, amp;
			sig = In.ar(in);
			amp = Amplitude.kr(sig, attack, release, mul, add);
			Out.kr(out, amp);
		}).send(s);
		
		SynthDef.new(\inputfreq, {arg in=0, out=0; // target bus should have two channels! 
			var sig;
			sig = In.ar(in, 1);
			Out.kr(out, Pitch.kr(sig));
		}).send(s);
		
		SynthDef.new(\patch_limiter_pan, {arg in=0, out=0, pre=1.0, post=1.0, thresh=1.0, attack=0.01, release=0.1, pan=0.0;
			var input, limited;
			input = In.ar(in, 1) * pre;
			limited = Compander.ar(input, input, thresh, 1.0, 0.0, attack, release, post);
			ReplaceOut.ar(out, Pan2.ar(limited, pan));
		}).send(s);	
		
		0.25.wait;
		
		//---------------------------------------------------------- audio structure
		//input/analysis/output groups
		ig = Group.new(s);
		xg = Group.new(ig, \addAfter);
		og = Group.new(xg, \addAfter);
		
		//input/analysis/output busses
		in_b = Bus.audio(s, 1);
		freq_b = Bus.control(s, 2);
		amp_b = Bus.control(s, 1);
		out_b = Bus.audio(s, 1);
		out2_b = Bus.audio(s, 1);
		
		//input/analysis/output synths
		in_s = Synth.new(\in_adc, [\in, inputchannel, \out, in_b.index], ig, \addToHead);
		freq_s = Synth.new(\inputfreq, [\in, in_b.index, \out, freq_b.index], ig, \addToTail);
		amp_s = Synth.new(\inputamp, [\in, in_b.index, \out, amp_b.index, \release, 0.08], ig, \addToTail);
		out1_s = Synth.new(\patch_pan, [\in, out_b.index, \out, outputchannel, \pan, -0.4], og, \addToHead);
		out2_s = Synth.new(\patch_mono, [\in, out2_b.index, \out, outputchannel + 1], og, \addToHead);
		
		//------------------------------------------------------ wavetables
		vosc_bufs = Buffer.allocConsecutive
		(	4, s, 1024, 1,
			{	arg buf, i;
					var sa;
				sa = Array.fill
				(	i+1,
					{	arg j;
						var val;
						if (j<i, {val=0}, {val=1});
						//val.postln
					}
				);
				buf.sine1Msg(sa);
			}
		);
				
		//----------------------------------------------- delay buffers
		delay_bufs = Array.fill(4, {
			Buffer.alloc(s, s.sampleRate * 40.0, 1);
		});
		
		0.25.wait;
		
		//--------------------------------------------------- delay synths
		echo_s = Array.fill
		(	4,
			{	arg i;
				var synth;
				synth = Synth.new
				(	\shapedelay_env,
					[	\delbuf, delay_bufs[i],
						\in, in_b.index,
						\maxdelaytime, 30,
						\delaytime, 20,
						\attack, 10 + (30 * i),
						\level, -40.dbamp,
						\shapebuf, vosc_bufs[i].bufnum,
						\gate, 0
					], xg, \addToTail
				);
				if ((i&2) == 0, {
					synth.set(\out, out_b.index);
				}, {
					synth.set(\out, out2_b.index);
				});
				synth
			}
		);
		
		//------------------------------------------------------- oscillator function, vars, params
		sines = Array.newClear(12);
		whichsine = 0;
		attacktime = 10;
		newosc = 
		{	arg freq, amp, time;
			SystemClock.sched
			(	time,
				{	
					if (sines[whichsine].isNil,
					{
						var t1, t2, t3;
						sines[whichsine] = nil;
						t1 = 0.8.rand;
						t2 = 0.8.rand;
						t3 = 0.8.rand;
						//("this time: " ++ time).postln;
						sines[whichsine] = Synth.new
						(	\vorg_1shot, 
							[	\freq, freq,
								\level, (amp * 0.05 + 0.03) * 0.25,
								\attack, time.min(180.0),
								\dur, (time * 6.0).min(360.0),
								\release, (time * 4.0).min(300.0),
								\timbre_offset, 0,
								\timbre1_0, t1,
								\timbre1_1, (t1 + (1.0-t1).rand2).abs,
								\timbre2_0, t2,
								\timbre2_1, (t2 + (1.0-t2).rand2).abs,
								\timbre3_0, t3,
								\timbre3_1, (t3 + (1.0-t3).rand2).abs,
								\panfactor1, 1.0.rand2;
								\panfactor2, 1.0.rand2;
								\basebuf, vosc_bufs[0].bufnum,
								\out, out_b.index
							], xg, \addToHead
						);
					},
					{	// synth exists
						if (	sines[whichsine].isRunning, 
						{	sines[whichsine].set(\freq, freq);
						},
						{	
							var t1, t2, t3;
							sines[whichsine] = nil;
							t1 = 0.8.rand;
							t2 = 0.8.rand;
							t3 = 0.8.rand;
							//("this time: " ++ time).postln;
							sines[whichsine] = Synth.new
							(	\vorg_1shot, 
								[	\freq, freq,
									\level, amp * 0.05 + 0.03,
									\attack, time.min(180.0),
									\dur, (time * 6.0).min(360.0),
									\release, (time * 4.0).min(300.0),
									\timbre_offset, 0,
									\timbre1_0, t1,
									\timbre1_1, (t1 + (1.0-t1).rand2).abs,
									\timbre2_0, t2,
									\timbre2_1, (t2 + (1.0-t2).rand2).abs,
									\timbre3_0, t3,
									\timbre3_1, (t3 + (1.0-t3).rand2).abs,
									\basebuf, vosc_bufs[0].bufnum,
									\out, out_b.index
								], xg, \addToHead
							);
						});
					});
					whichsine = (whichsine + 1) % sines.size;
					nil;
				}
			);
		};
		
		//---------------------------------------------------------- pitch follow function, vars, params
		amp_thresh = (-16.dbamp);
		amp_t_thresh = 4;
		wasinrange = false;
		
		targetpitch = 0;
		lastnote = 0;
		lasthighamp = 0;
		dur = 0;
		whichecho = 0;
		
		delta_t = 0.2;
		
		pitch_amp_logic = Task.new
		({	inf.do
			({	arg i;
				freq_b.get // asynchronous bus read, we have to wait
				({	arg freq;
					var pitch;
					//("FREQ: " + freq + ": " + freq.cpsmidi.round).postln;
					amp_b.get // wait again
					({	arg amp;
						var inrange;
						
						inrange =  (amp >= amp_thresh);
						//("AMP:  " ++ amp).postln;
						if (	inrange == false,
						{	
							{ if(signal_led.notNil, { signal_led.background_(Color.black;); }); }.defer;
							if ( wasinrange, // ending the last duration
							{	SystemClock.sched
								(0, 
									{	arg time; 
										dur= time - lasthighamp;
										("ENDING OF AUDIBLE EVENT, DURATION " + dur).postln;
										echo_s[whichecho].set(\delaytime, dur * 4);
										whichecho = (whichecho + 1) % (echo_s.size);
										nil
									}
								);
							});
							//lastpitch = freq.cpsmidi.round(1);  
							wasinrange = inrange;	
							//"TOO QUIET".postln;
						},
						{	//"LOUD ENOUGH".postln;
							{ if(signal_led.notNil, { signal_led.background_(Color.white;); }); }.defer;
							SystemClock.sched
							(0,{	arg time;
								if ( wasinrange == false, // starting a new duration
								{	
									lasthighamp = time;
									targetpitch = freq.cpsmidi.round(1);
								});
								if (	(time-lasthighamp > amp_t_thresh) && (time-lastnote > amp_t_thresh),
								{	
									pitch = freq.cpsmidi.round(1);
									if(pitch == targetpitch, 
									{
										"NEW VOICE".postln;
										("TIME UNIT : "+dur + ", HZ : " + freq).postln;
										newosc.value(freq, amp, dur);
										lastnote = time;
									}, {
										targetpitch = pitch;
										//lasthighamp = time;
									});
								}, { 
									//box_set.value(0, 2, 0)
								});
								echo_s[whichecho].set(\freq, freq * 8);
								//lastpitch = pitch;
								wasinrange = inrange;
								nil // don't reschedule
							});
						});
					});
				});
				delta_t.wait;
			});
		});
		
		
		100.do({".".postln; 0.002.wait;});
		
		"
		this is the program called REMAINS [0.2].
		
		
		please sing or play long tones.
		
		
		include silences.
		
		
		remain within a single chromatic pitch class for several seconds.
		
		.
		
		.
		
		catfact.net 
		
		
		".postln;
		
		
		//-------------------------------------------------- begin
		pitch_amp_logic.play;
		echo_s.do({|syn, i| syn.set(\gate, 1)});
		
	
		//------------------------------------------- LIMITER
		out_limiter_patch_1 = Synth.new(\patch_limiter_pan, [
			\in, out_b.index, \out, out_b.index,
			\pre, 12.dbamp,
			\thresh, -6.dbamp,
			\post, 12.dbamp,
		], og, \addBefore);
		
		out_limiter_patch_2 = Synth.new(\patch_limiter_pan, [
			\in, out2_b.index, \out, out2_b.index,
			\pre, 12.dbamp,
			\thresh, -6.dbamp,
			\post, 12.dbamp,
			\pan, 1.0
		], og, \addBefore);
		
		
		out_limiter_patch_1.set(\pan, -1);
		//newosc.value(140, 0.4, 10.0);
		
		/////////////////////////////////////////////////////----------------------------------- gui
		
		0.1.wait;
		
		{ // appclock deferral
		
		Document.listener.bounds = Rect(0, 0, 680, 380);
		
		remains_logic_win = SCWindow("LISTENER", Rect(0, 400, 320, 370));
		remains_logic_win.front;
		
		pollperiod_label = SCStaticText(remains_logic_win, Rect(0, 0, 220, 74));
		pollperiod_label.string = "POLLING PERIOD s";
		pollperiod_label.background = Color.new(0.7, 0.7, 0.7);
		pollperiod_label.align = \center;
		pollperiod_label.font = Font("Times", 24);
		
		pollperiod_numbox = SCNumberBox(remains_logic_win, Rect(220, 0, 100, 74));
		pollperiod_numbox.background = Color.new(0.8, 0.8, 0.8);
		pollperiod_numbox.clipLo_(0.02);
		pollperiod_numbox.clipHi_(2.0);		
		pollperiod_numbox.scroll_step_(0.02);
		pollperiod_numbox.value_(delta_t);
		pollperiod_numbox.font = Font("Times", 24);
		pollperiod_numbox.align = \center;
		// pollperiod_numbox.radius = 0.0;
		
		timethreshold_label = SCStaticText(remains_logic_win, Rect(0, 74, 220, 74));
		timethreshold_label.string = "TIME THRESHOLD s";
		timethreshold_label.align = \center;
		timethreshold_label.font = Font("Times", 22);
		
		timethreshold_numbox = SCNumberBox(remains_logic_win, Rect(220, 74, 100, 74));
		timethreshold_numbox.background = Color.new(0.9, 0.9, 0.9);
		timethreshold_numbox.clipLo_(0.2);
		timethreshold_numbox.clipHi_(20.0);
		timethreshold_numbox.scroll_step_(0.05);
		timethreshold_numbox.value_(amp_t_thresh);
		timethreshold_numbox.font = Font("Times", 24);
		timethreshold_numbox.align = \center;
		//timethreshold_numbox.radius = 0.0;
			
		ampthreshold_label = SCStaticText(remains_logic_win, Rect(0, 148, 220, 74));
		ampthreshold_label.string = "AMP THRESHOLD dB";
		ampthreshold_label.align = \center;
		ampthreshold_label.font = Font("Times", 22);
		ampthreshold_label.background = Color.new(0.7, 0.7, 0.7);
		
		ampthreshold_numbox = SCNumberBox(remains_logic_win, Rect(220, 148, 100, 74));
		ampthreshold_numbox.background = Color.new(0.8, 0.8, 0.8);
		ampthreshold_numbox.clipLo_(-60);
		ampthreshold_numbox.clipHi_(0);
		ampthreshold_numbox.scroll_step_(0.5);
		ampthreshold_numbox.value_(amp_thresh.ampdb);
		ampthreshold_numbox.font = Font("Times", 24);
		ampthreshold_numbox.align = \center;
		//ampthreshold_numbox.radius = 0.0;
		
		signal_label = SCStaticText(remains_logic_win, Rect(0, 222, 220, 74));
		signal_label.string = "SIGNAL";
		signal_label.align = \center;
		signal_label.font = Font("Times", 24);
		signal_led = SCTextField(remains_logic_win, Rect(220, 222, 100, 74));
		signal_led.background_(Color.black;);
		
		storedefaults_button = SCButton(remains_logic_win, Rect(0, 296, 320, 74))
			.states_([["store defaults", Color.black, Color.new(0.5, 0.9, 0.5)]])
			.action_({
				defaults_file = File(String.scDir.dirname ++ "/remains_defaults.bin", "wb");
				defaults_file.putFloat(delta_t);
				defaults_file.putFloat(amp_t_thresh);
				defaults_file.putFloat(amp_thresh.ampdb);
				defaults_file.putFloat(lim_pregain_db);
				defaults_file.putFloat(lim_thresh_db);
				defaults_file.putFloat(lim_postgain_db);
				defaults_file.close;
			});
		storedefaults_button.font = Font("Times", 24);
				
		pollperiod_numbox.action = { |numb|
			delta_t = numb.value;
		};
		
		timethreshold_numbox.action = { |numb|
			amp_t_thresh = numb.value;
		};
		
		ampthreshold_numbox.action = { |numb|
			amp_thresh = numb.value.dbamp;
		};
		
		remains_ctl_win = SCWindow("LIMITER", Rect(360, 400, 320, 370));
		remains_ctl_win.front;
		
		pregain_label = SCStaticText(remains_ctl_win, Rect(5, 5, 100, 25));
		pregain_label.string = "PREGAIN dB";
		pregain_label.align = \center;
		thresh_label = SCStaticText(remains_ctl_win, Rect(110, 5, 100, 25));
		thresh_label.string = "THRESHOLD dB";
		postgain_label = SCStaticText(remains_ctl_win, Rect(215, 5, 100, 25));
		postgain_label.string = "POSTGAIN dB";
		
		pregain_numbox = SCNumberBox(remains_ctl_win, Rect(5, 35, 100, 25));
		pregain_numbox.background = Color.new(0.9, 0.9, 0.9);
		pregain_numbox.font = Font("Times", 24);
		pregain_numbox.align = \center;
		// pregain_numbox.radius = 0.0;
		thresh_numbox = SCNumberBox(remains_ctl_win, Rect(110, 35, 100, 25));
		thresh_numbox.background = Color.new(0.9, 0.9, 0.9);
		thresh_numbox.font = Font("Times", 24);
		thresh_numbox.align = \center;
		//thresh_numbox.radius = 0.0;
		postgain_numbox = SCNumberBox(remains_ctl_win, Rect(215, 35, 100, 25));
		postgain_numbox.background = Color.new(0.9, 0.9, 0.9);
		postgain_numbox.font = Font("Times", 24);
		postgain_numbox.align = \center;
		//postgain_numbox.radius = 0.0;
		
		pregain_slider = SmoothSlider(remains_ctl_win, Rect(5, 65, 100, 300));
		pregain_slider.knobColor = Color.black;
		pregain_slider.hiliteColor = Color.black;
		pregain_slider.knobSize = 0;
		thresh_slider = SmoothSlider(remains_ctl_win, Rect(110, 65, 100, 300));
		thresh_slider.knobColor = Color.black;
		thresh_slider.hiliteColor = Color.black;
		thresh_slider.knobSize = 0;
		postgain_slider = SmoothSlider(remains_ctl_win, Rect(215, 65, 100, 300));
		postgain_slider.knobColor = Color.black;
		postgain_slider.hiliteColor = Color.black;
		postgain_slider.knobSize = 0;
		
		gain_spec = [-24.0, 24.0, \lin, 0.25, 0.0].asSpec;
		thresh_spec = [-32.0, 0.0, \lin, 0.25, -6.0].asSpec;
		
		pregain_numbox.action = {|numb| var db, amp;
			db = gain_spec.constrain(numb.value);
			pregain_slider.value = gain_spec.unmap(db); 
			lim_pregain_db = db;
			out_limiter_patch_1.set(\pre, lim_pregain_db.dbamp);
			out_limiter_patch_2.set(\pre, lim_pregain_db.dbamp);
			numb.value = db;
		};
		
		thresh_numbox.action = {|numb| var db;
			db = thresh_spec.constrain(numb.value);
			thresh_slider.value = thresh_spec.unmap(db); 
			lim_thresh_db = db;
			out_limiter_patch_1.set(\thresh, lim_thresh_db.dbamp);
			out_limiter_patch_2.set(\thresh, lim_thresh_db.dbamp);
			numb.value = db;
		};
		
		postgain_numbox.action = {|numb| var db;
			db = gain_spec.constrain(numb.value);
			postgain_slider.value = gain_spec.unmap(db); 
			lim_postgain_db = db;
			out_limiter_patch_1.set(\post, lim_postgain_db.dbamp);
			out_limiter_patch_2.set(\post, lim_postgain_db.dbamp);
			numb.value = db;
		};
		
		pregain_slider.action = { var db;
			db = gain_spec.map(pregain_slider.value);
			pregain_numbox.valueAction_(db);
		};
		
		thresh_slider.action = { var db;
			db = thresh_spec.map(thresh_slider.value);
			thresh_numbox.valueAction_(db);
		};
		
		postgain_slider.action = { var db, amp;
			db = gain_spec.map(postgain_slider.value);
			postgain_numbox.valueAction_(db);
		};
		
		// read defaults
		defaults_file = File(String.scDir.dirname ++ "/remains_defaults.bin", "rb");
		pollperiod_numbox.valueAction_(defaults_file.getFloat.round(0.00000001)); // why the tiny offset? VERY ANNOYING
		timethreshold_numbox.valueAction_(defaults_file.getFloat);
		ampthreshold_numbox.valueAction_(defaults_file.getFloat);
		pregain_numbox.valueAction_(defaults_file.getFloat);
		thresh_numbox.valueAction_(defaults_file.getFloat);
		postgain_numbox.valueAction_(defaults_file.getFloat);
		defaults_file.close;
		
		}.defer; // gui shit goes to the appclock
		
		}.play; // init routine
	}
	
	kill {
	
		ig.free;
		xg.free;
		og.free;
		
		in_b.free;
		freq_b.free;
		amp_b.free;
		out_b.free;
		out2_b.free;
		
		pitch_amp_logic.stop;
		
		remains_ctl_win.close;
		remains_logic_win.close;
	}
	
	setinputchannel {
		arg chan;
		inputchannel = chan;
		in_s.set(\in, inputchannel);
	}

	setoutputchannel {
		arg chan;
		outputchannel = chan;
		out1_s.set(\in, outputchannel);
		out2_s.set(\in, outputchannel + 1);
	}
	
}