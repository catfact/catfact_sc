Grandel {
	
	var s;
	var <>inputchannel, <>outputchannel;
	var <>buf, <>in_b, <>phase_b;
	var <>in_s, <>phase_s, <>bufwr_s;
	
	var <>gg;
	
	var <>wait=1.0,
		<>deltime=1.0,
		<>rate=1.0,
		<>dur = 4.0,
		<>attack = 1.0,
		<>release = 1.0;
		
	var gran_r;
	 
	
	*new {
		arg server, inchannel, outchannel, target, addaction;
		^super.new.init(server, inchannel, outchannel, target, addaction);
	}
	
	init {
		arg server, inchannel, outchannel, target, addaction=\addAfter;
		inputchannel = inchannel;
		outputchannel = outchannel;
		
		s = server;	
				
		Routine {
			
			SynthDef.new(\adc, {
				arg in=0, out=0;
				Out.ar(out, SoundIn.ar(in));
			}).store;
			
			SynthDef.new(\patch_mono, {
				arg in=0, out=0, amp=1.0;
				Out.ar(out, In.ar(in) * amp);
			}).store;
			
			
			SynthDef.new(\buf_phasor, {
				arg buf, out, rate=1.0;
				Out.ar(out, Phasor.ar(0, rate * BufRateScale.kr(buf), 0.0, BufFrames.kr(buf)));
			}).store;
			
			SynthDef.new(\bufwr_mono, {
				arg in, buf, phasebus;
				BufWr.ar(In.ar(in, 1), buf, In.ar(phasebus, 1));
			}).store;
			
			SynthDef.new(\buf_pos_offset_grain, {
				arg out=0, buf=0, amp=1.0,
					attacktime=0.0, sustaintime=1.0, releasetime=0.0, curve=0.0,
					phasebus=0, rate=1.0, offsetframes = -1.0;
				
				var frames, pos, sig, env;
				frames = BufFrames.kr(buf);
				pos = DC.ar(In.ar(phasebus)) + offsetframes;
				pos = pos.wrap(0.0, frames);
				sig = PlayBuf.ar(1, buf, rate, 0, pos, 1);
				env = EnvGen.ar(
					Env.new([0.0, 1.0, 1.0, 0.0], [attacktime, sustaintime, releasetime], curve),
					doneAction:2
				);
				Out.ar(out, env * amp * sig);
			}).store;
			
			 s.sync;
			//0.1.wait;
						
						
			buf = Buffer.alloc(s, s.sampleRate * 30.0, 1);
			
			in_b = Bus.audio(s, 1);
			phase_b = Bus.audio(s, 1);
			
			in_s = Synth.new(\patch_mono, [\in, inputchannel, \out, in_b.index], target, addaction);
			phase_s = Synth.new(\buf_phasor, [\buf, buf.bufnum, \out, phase_b.index], in_s, \addAfter);
			bufwr_s = Synth.new(\bufwr_mono, [\in, in_b.index, \buf, buf.bufnum, \phasebus, phase_b.index], phase_s, \addAfter);
			
			gg = Group.after(bufwr_s, \addAfter);
			
			gran_r = Routine({ inf.do ({
				Synth.new(\buf_pos_offset_grain, [
					\attacktime, attack, \sustaintime, dur - attack - release, \releasetime, release,
					\buf, buf.bufnum,
					\phasebus, phase_b.index,
					\offsetframes, deltime * s.sampleRate * -1.0,
					\rate, rate,
					\out, outputchannel
				], gg, \addToTail);
				wait.wait;
			}) });
			
			gran_r.play;
			
		}.play; // init routine
	} // init funciton
} // Grandel class