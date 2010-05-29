CfRemains {
	var s;
	var ig, xg, og;
	var in_b, freq_b, amp_b, out_b, out2_b;

	var in_s, in2_s;
	var out1_s, out2_s;
	var freq_s, amp_s;
	
	var vosc_bufs, delay_bufs;
	var echo_s;
	var sines, whichsine, attacktime;
	
	var newsinewave;
	
	var <>amp_thresh, <>amp_t_thresh, wasinrange;
	var lastfreq, lastnote, lasthighamp;
	
	var dur;
	var whichecho;
	
	var <>delta_t;
	
	var pitch_amp_logic_task;
	
	var out_limiter_patch_1, out_limiter_patch_2;
	
	var lim_pregain_db, lim_thresh_db, lim_postgain_db;	
	
		
	*new {
		arg server, inputchannel;
		^super.new.init(server);
	}

	init { arg server;
	
		s = server;	
		
		Routine {
	
		
		//-------------------------------------------------------------- synthdefs
		SynthDef.new(\vorg_1shot, 
		{	arg out=0, timbre1_0, timbre1_1, timbre2_0, timbre2_1, timbre3_0, timbre3_1, freq=440, gate=1, level=0.1, attack=0.1, release=0.1, dur=10, filter_level=0.0, noise_level = 0.0, sub_level=1.0, numbufs=4, basebuf=0, panfactor1 = 0.0, panfactor2 = 0.0;
			var timbre1, timbre2, timbre3, sin, saw, ring, saws, amp, saw_amp, env, noise, filter;
			timbre1 = Line.kr(timbre1_0, timbre1_1, attack + dur + release);
			timbre2 = Line.kr(timbre2_0, timbre2_1, attack + dur + release);
			timbre3 = Line.kr(timbre3_0, timbre3_1, attack + dur + release);
			env = Env.linen(attack, dur, release, 1, 'lin');
			amp = EnvGen.ar(env, gate, 1, doneAction:2);
			saw_amp = EnvGen.ar(env, gate, timeScale:2.0);
			saw = Pan2.ar(SyncSaw.ar(freq, freq * (2 ** (3 * timbre2))), panfactor2);
			sin = Mix.new([Pan2.ar(VOsc.ar((timbre1 * numbufs) + basebuf, freq), panfactor1), Pan2.ar(FSinOsc.ar(freq * 0.5) * sub_level, -1 * panfactor1)]);
			ring = Pan2.ar(Mix.ar([sin * saw]), panfactor2 * -1);
			saws = Mix.new([saw * timbre3, ring * timbre3]) * saw_amp;
			filter = LPF.ar(Mix.new([sin, saws]), freq + (filter_level * (9000-freq)));
			Out.ar(out, filter * amp * level);
		}).send(s);
		
	
		
		SynthDef.new(\shapedelay_env, {arg in=0, out=1, delbuf=0, shapebuf=0, delaytime=0, level=1, freq=440, attack=1, release=1, gate=1, lpf_freq=2000, hpf_freq=80;
			var sig, amp, env;
			sig = HPF.ar(LPF.ar(BufDelayN.ar(delbuf, In.ar(in, 1), delaytime), lpf_freq), hpf_freq);
			sig = Shaper.ar(shapebuf, sig);
			sig = RLPF.ar(sig, Lag.kr(freq, delaytime), 0.4); 
			env = Env.asr(attack, 1, release);
			amp = EnvGen.ar(env, gate, 1); // sustain forever, doneAction:2);
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
		
		
		
		SynthDef.new("patch_pan", {arg in=0, out=0, amp=1.0, pan=0, amplag = 0.0;
			var input;
			input = In.ar(in, 1);
			amp = Lag.kr(amp, amplag);
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
		
		SynthDef.new(\patch_limiter_pan, {arg in=0, out=0, pre=1.0, post=1.0, thresh=1.0, attack=0.01, release=0.1, pan=0.0, amp=0.0, amplag=0.0;
			var input, limited;
			amp = Lag.kr(amp, amplag);
			input = In.ar(in, 1) * pre;
			limited = Compander.ar(input, input, thresh, 1.0, 0.0, attack, release, post);
			ReplaceOut.ar(out, Pan2.ar(limited * amp, pan));
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
		
		in_s = Synth.new(\in_adc, [\in, 0, \out, in_b.index], ig, \addToHead);
		freq_s = Synth.new(\inputfreq, [\in, in_b.index, \out, freq_b.index], ig, \addToTail);
		amp_s = Synth.new(\inputamp, [\in, in_b.index, \out, amp_b.index, \release, 0.08], ig, \addToTail);
		out1_s = Synth.new(\patch_pan, [\in, out_b.index, \out, 4, \pan, -0.4], og, \addToHead);
		out2_s = Synth.new(\patch_mono, [\in, out2_b.index, \out, 5], og, \addToHead);
		out1_s.set(\out, 0);
		out2_s.set(\out, 1);
		
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
						\level, 0.018,
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
		newsinewave = 
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
		
		lastfreq = 0;
		lastnote = 0;
		lasthighamp = 0;
		dur = 0;
		whichecho = 0;
		
		delta_t = 0.2;
		
		pitch_amp_logic_task = Task.new
		({	inf.do
			({	arg i;
				freq_b.get // asynchronous bus read, we have to wait
				({	arg freq;
					//("FREQ: " + freq + ": " + freq.cpsmidi.round).postln;
					amp_b.get // wait again
					({	arg amp;
						var inrange;
						inrange =  (amp >= amp_thresh);
						//("AMP:  " ++ amp).postln;
						if (	inrange == false,
						{	
							if ( wasinrange, // ending the last duration
							{	SystemClock.sched
								(0, 
									{	arg time; 
										dur= time - lasthighamp;
										("ENDING OF AUDIBLE EVENT, DURATION " + dur).postln;
										echo_s[whichecho].set(\delaytime, dur * 4);
										whichecho = (whichecho + 1) % (echo_s.size);
										lastfreq = freq; 
										wasinrange = inrange; 
											///
											//
											// false
										wasinrange = false;
										nil
									}
								);
							});
							///being safe... yuk
							lastfreq = freq; 
							wasinrange = inrange;	
							//"TOO QUIET".postln;
						},
						{	//"LOUD ENOUGH".postln;
							SystemClock.sched
							(0,{	arg time;
								if ( wasinrange == false, // starting a new duration
								{	lasthighamp = time;
								});
								if (	(time-lasthighamp > amp_t_thresh) && (time-lastnote > amp_t_thresh), // longenough->sine
								{	"NEW VOICE".postln;
									("TIME UNIT : "+dur + ", HZ : " + freq).postln;
									newsinewave.value(freq, amp, dur);
									lastnote = time;
									//box_set.value(0, 2, 1);
								}, { 
									//box_set.value(0, 2, 0)
								});
								echo_s[whichecho].set(\freq, freq * 8);
								lastfreq = freq; 
								wasinrange = inrange;
								nil // don't reschedule
							});
						});
					});
				});
				delta_t.wait;
			});
		});
		
		
		//-------------------------------------------------- begin
		pitch_amp_logic_task.play;
		
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

		}.play; // init routine
	}
	
	setamplag { arg val;
		out_limiter_patch_1.set(\amplag, val);
		out_limiter_patch_2.set(\amplag, val);
	}
	
	setamp { arg val;
		out_limiter_patch_1.set(\amp, val);
		out_limiter_patch_2.set(\amp, val);
	}
	
	setlimthresh {
	}
	
	setlimpregain {
	}
	
	setlimpostgain {
	}
	
	setinputchannel {
	}
	
	kill {
		[ig, xg, og].free;
		[in_b, freq_b, amp_b, out_b, out2_b].free;
		[in_s, in2_s].free;
		[out1_s, out2_s].free;
		[freq_s, amp_s].free;
		[vosc_bufs, delay_bufs].free;
		echo_s.free;
		pitch_amp_logic_task.stop;
		[out_limiter_patch_1, out_limiter_patch_2].free;
	}
}

CfRemainsGUI {
	
}