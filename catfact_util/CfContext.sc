CfAudioContext {
	var <s;
	var <nch;
	var <ig, <xg, <og;
	var <in_b, <in_s, <out_b, <out_s;
	*new { arg serv, numchannels, bootfunc;
			^super.new.init(serv, numchannels, bootfunc)
	}
	init { arg serv, numchannels, bootfunc;
		s = serv;
		nch = numchannels;
		s.waitForBoot {
			Routine {

				// synthdefs
				SynthDef.new(\adc, {arg in=0, out=0, amp=1.0;
					Out.ar(out, SoundIn.ar(in) * amp);
				}).send(s);	

				SynthDef.new(\patch, {arg in=0, out=0, amp=1.0;
					Out.ar(out, In.ar(in) * amp);
				}).send(s);
					
				SynthDef.new(\pan, {arg in=0, out=0, amp=1.0, pan=0.0;
					Out.ar(out, Pan2.ar(In.ar(in) * amp, pan));
				}).send(s);
				
				SynthDef.new(\delay, { arg in=0, out=0, maxtime=1.0, time=0.2;
					Out.ar(out, DelayL.ar(In.ar(in), maxtime, time));
				}).send(s);
				
				SynthDef.new(\lpf_ins, { arg bus=0, out=0, hz=10000, hzlag=0.1;
					hz = Lag.kr(hz, hzlag);
					ReplaceOut.ar(bus, LPF.ar(In.ar(bus), hz));
				}).send(s);
				
				SynthDef.new(\hishelf_ins, { arg bus=0, out=0, hz=10000, rs=1.0, db=0.0;
					ReplaceOut.ar(bus, BHiShelf.ar(In.ar(bus), hz, rs, db));
				}).send(s);
				
				s.sync;	
				
				ig = Group.new(s);
				xg = Group.after(ig);
				og = Group.after(xg);
				
				in_b = Bus.audio(s, nch);
				out_b = Bus.audio(s, nch);
				in_s = Array.fill(nch, { |i| Synth.new(\adc, [\in, i, \out, in_b.index + i], ig) });
				out_s = Array.fill(nch, { |i| Synth.new(\patch, [\out, i, \in, out_b.index + i], og) });
				
				s.sync;
				
				bootfunc.value(this);
			}.play;
		}
	}
}