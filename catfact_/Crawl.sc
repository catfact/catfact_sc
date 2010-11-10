Crawl {
	var <>posinc, <>skip;
	var <>dur;
	var <>seqdur;
	var <>rate;
	var <>s, <>in_b, <>out_b, <>buf, <>rec_s;
	var <>rectapper;
	var <>atk=0.25, <>rel=0.25;
	var <>level=0.25;
	var <>grain_g;
	var <>pos=0.0, <>num;
	
	*new {
		arg server, target, inbus=0, outbus=0, maxdur=30.0;
		^super.new.init(server, target, inbus, outbus, maxdur);
	}
	
	init {
		arg server, target, inbus, outbus, maxdur;
		s = server;
		in_b = inbus;
		out_b = outbus;
		
		SynthDef.new(\rec, {arg in=0, buf=0, loop=0, run=0, trig=0;
			RecordBuf.ar(In.ar(in, 2), buf, run:run, loop:loop, trigger:trig);
		}).store;
		
		SynthDef.new(\crawlgrain, {
			arg out=0, level=1.0, buf=0, pos=0.0, rate=1.0, dur=1.0, atk=0.25, rel=0.25,
				panL0= -1, panL1= -1, panR0=1, panR1=1,
				lpfhz0 = 10000, lpfhz1=8000,
				lpfrq0=0.0, lpfrq1=1.0;
			var env, amp, bufframes, bufdur, bufsamplerate, phase, sig, panL, panR, lpf;
			env = Env.linen(dur*atk, dur*(1.0-atk-rel), dur*rel);
			amp = EnvGen.ar(env, gate:1, doneAction:2);
			bufframes = BufFrames.kr(buf);
			bufdur = BufDur.kr(buf);
			bufsamplerate = bufframes/bufdur;
			phase = Line.ar(pos * bufsamplerate, (pos+dur)*bufsamplerate, dur / rate);
			sig = BufRd.ar(2, buf, phase);
			/*
			sig = RLPF.ar(sig, Line.kr(lpfhz0, lpfhz1, dur), Line.kr(lpfrq0, lpfrq1, dur));
			*/
			lpf = MoogFF.ar(sig, XLine.kr(lpfhz0, lpfhz1, dur*0.7), Line.kr(lpfrq0, lpfrq1, dur*0.8));
			panL = Line.kr(panL0, panL1, dur);
			panR = Line.kr(panR0, panR1, dur);
			lpf = Mix.ar(Pan2.ar(lpf, [panL, panR]));
			
			Out.ar(out, Pan2.ar(lpf*amp*level, 0));
		}).store;
		
		buf = Buffer.alloc(s, s.sampleRate * maxdur, 2, {arg buf; this.continueInit(target)});
	}
	
	continueInit { arg target;
		grain_g = Group.after(target);
		rectapper = CfTapper.new;
		
	}
	
	startRecording {
		rec_s = Synth.new(\rec, [\in, in_b, \run, 1, \buf, buf], grain_g, \addBefore);
		rectapper.tap;
	}
	
	stopRecording { arg ratearg=1.0;
		pos = 0.0;
		rec_s.free;
		rate = ratearg;
		rectapper.tap;
		^(this.play);
	}
	
	play {
		var len = rectapper.time;
		// we want about 4 overlapping grains, that gives us:
		dur = len/2;
		skip = len/8;
		// we want the total sequence to be about 8* rec length
		seqdur = len* 8.0;
		// which gives us:
		num = (((seqdur - dur) / skip) - 1).floor;
		posinc = (len-dur)/num;
		
		Routine { num.do ({ arg i;
			var lpfrq0, lpfrq1, lpfhz0, lpfhz1,
				panL0, panL1, panR0, panR1;
			
			panL0 = i / num * -1.0;
			panL1 = (panL0 + (i * 0.25).wrap(0.0, 2.0)).wrap(-1.0, 1.0);
			
			panR0 = (panL0 + 2.0).wrap(-1.0, 1.0);
			panR1 = (panL1 + 2.0).wrap(-1.0, 1.0);
				
			lpfhz0 = 10000 * ((20/21)**(i * 8.0 / num));
			lpfhz1 = lpfhz0 * ((5/6)**(i * 8.0 / num));
			
			lpfrq0 = i/num * 0.5;
			lpfrq1 = i/num * 3.3;
			dur.postln;			
			Synth.new(\crawlgrain, [
				\buf, buf,
				\out, out_b,
				\pos, pos,
				\dur, dur,
				\rate, rate,
				\level, level,
				\atk, atk,
				\rel, rel,
				\lpfhz0, lpfhz0,
				\lpfhz1, lpfhz1,
				\lpfrq0, lpfrq0,
				\lpfrq1, lpfrq1,
				\panL0, panL0,
				\panL1, panL1,
				\panR0, panR0,
				\panR1, panR1
			], grain_g);
			pos = pos + posinc;
			skip.wait;
		}); }.play;
		^seqdur
	}
}