Jalop {
	var s;
	var <>amp;
	var <window, <view, <width, <height;
	var <bufs;
	var <numbufs=8, <buflen=32.0;
	var watchF, watchR;	
	var rec, switch, curbuf, dur=1.0;
	var in=0;
	
	*new{ arg server;
		^super.new.init(server);
	}
	
	init {
		arg server;
		s = server;
		Routine {
	
			switch = CfTapper.new;
			curbuf = 0;
			
			~watchF = Dictionary.new;
			~watchR = Dictionary.new;
			['/n_go', '/n_end', '/n_off', '/n_on'].do({
				arg cmd;
				~watchF.add(cmd -> {arg t, r, msg; [t, r, msg].postln; });
				~watchR.add(cmd -> OSCresponderNode(s.addr, cmd, {
					arg t, r, msg; 
					// ~watchF[cmd].value(t, r, msg);
				}).add );
			});
			
			bufs = Array.fill(8, { Buffer.alloc(s, s.sampleRate * buflen, 1); });
			curbuf = numbufs - 1;
			
			0.1.wait;
			
			SynthDef.new(\bufwr_1, { arg buf, in=0, channels=2;
				BufWr.ar(
					// Mix.new(SoundIn.ar[in, in+1]),
					Mix.new(SoundIn.ar([0, 1])),
					buf,
					Line.ar(0, BufFrames.kr(buf), BufDur.kr(buf), doneAction:2)
				)
			}).send(s);
			
			SynthDef.new(\jalop, {
				arg buf=0, out=0, pan=0,
					rate=1.0, skipHz=2.0, skipRatio=0.5, slewSamps=16.0,
					atk=0.1, sus=8.0, loopdur=8.0, rel=4.0, slopecomp=1.0,
					amp=1.0;
				var output, phase, skip, bufSr, ampenv;
				ampenv = EnvGen.ar(Env.new([0.0, 1.0, 1.0, 0.0], [atk, sus, rel]), doneAction:2);
				bufSr = BufSampleRate.kr(buf);
				phase = Sweep.ar(1, bufSr * rate);
				skip = Latch.ar(Sweep.ar(1, bufSr * rate * skipRatio), LFPulse.ar(skipHz));
				phase = (phase - skip).wrap(0, min(loopdur * SampleRate.ir, BufFrames.kr(buf)));
				phase = Slew.ar(phase, bufSr * slewSamps, bufSr * slewSamps);
				output = BufRd.ar(1, buf, phase, loop:1, interpolation:4);
				output = output * Slew.ar(1 / (1.0 + (slopecomp * (Slope.ar(phase) / 44100.0))), 1.0);
				output = LPF.ar(output, 8000);
				output = output + LPF.ar(output, 800);
				Out.ar(out, Pan2.ar(output * ampenv * amp, pan));                 
			}).send(s);
		
		}.play // init routine
	} // init
	
	record {
		curbuf = (curbuf + 1) % numbufs;
		rec = Synth.new(\bufwr_1, [\buf, bufs[curbuf].bufnum, \in, in], s);
		switch.tap;
	}
	
	stopRecord {
		rec.free;
		dur = switch.tap;
		dur.postln;
	}
	
	play {	
		arg atk, sus, rel,
			args = [	[ 1.0, 0.25, 19/32, 64],
					[ 1.0, 0.2, 5/8, 128]	];
					//[ 	[ rate, skiphz, skipratio, slewsamps] , [...] ... ] 
					
		dur.postln;	
		args.do({ arg arr, i;
			arr.postln;
			Synth.new(\jalop, [
				\buf, bufs[curbuf].bufnum,
				\rate, arr[0],
				\skipHz, arr[1],
				\skipRatio, arr[2],
				\slewSamps, arr[3],								\loopdur, dur,
				\atk, dur,
				\sus, sus,
				\rel, rel,
				\amp, amp
			]);
		});
		
	}
			
} // Jalop

/*
ArgsTest {

	*new {
		^super.new.init;
	}
	
	init {
	}
	
	func { arg args = [ [0, 0], [0, 0] ];
		args.do({arg arr; arr.do({arg it; it.postln; })});
	
	}
}
*/