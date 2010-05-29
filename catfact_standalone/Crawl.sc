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
			arg out=0, level=1.0, buf=0, pos=0.0, rate=1.0, dur=1.0, atk=0.25, rel=0.25;
			var env, amp, bufframes, bufdur, bufsamplerate, phase, sig;
			env = Env.linen(dur*atk, dur*(1.0-atk-rel), dur*rel);
			amp = EnvGen.ar(env, doneAction:2);
			bufframes = BufFrames.kr(buf);
			bufdur = BufDur.kr(buf);
			bufsamplerate = bufframes/bufdur;
			phase = Line.ar(pos * bufsamplerate, (pos+dur)*bufsamplerate, dur);
			sig = BufRd.ar(2, buf, phase);
			Out.ar(out, Pan2.ar(sig*amp*level, 0));
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
		^(this.play(rectapper.time));
	}
	
	play {
		arg len;
		// we want about 4 overlapping grains, that gives us:
		dur = len/2;
		skip = len/8;
		// we want the total sequence to be about 8* rec length
		seqdur = len* 8.0;
		// which gives us:
		num = (((seqdur - dur) / skip) - 1).floor;
		posinc = (len-dur)/num;
		
		Routine { num.do ({ arg i;
			Synth.new(\crawlgrain, [
				\buf, buf,
				\out, out_b,
				\pos, pos,
				\dur, dur,
				\rate, rate,
				\level, level,
				\atk, atk,
				\rel, rel
			], grain_g);
			pos = pos + posinc;
			skip.wait;
		}); }.play;
		^(dur*8.0)
	}
}