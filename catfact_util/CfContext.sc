/// CATFACT 
/// audio boilerplate

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
				
				
				SynthDef.new(\lag_ins_kr, { arg bus=0, out=0, lag=0.001;
					ReplaceOut.kr(bus, Lag.kr(In.kr(bus), lag));
				}).send(s);
				
				SynthDef.new(\pulse_kr, { arg out=0, hz=4.0, hzlag=0.001;
					hz = Lag.kr(hz, hzlag);
					Out.kr(out, LFPulse.kr(hz));
				}).send(s);
				
				SynthDef.new(\hishelf_ins, { arg bus=0, out=0, hz=10000, rs=1.0, db=0.0;
					ReplaceOut.ar(bus, BHiShelf.ar(In.ar(bus), hz, rs, db));
				}).send(s);
				
				SynthDef.new(\rec, { arg in=0, buf=0, loop=1, run=1, pre=0, trig=0, done=0;
					RecordBuf.ar(In.ar(in), buf, loop:loop, run:run, preLevel:pre, trigger:trig, doneAction:done);
				}).send(s);
				
								
				SynthDef.new(\bufdelay_fb, {
					arg in=0, 
						out=0,
						buf=0,
						amp = 1.0,
						inputamp = 1.0,
						delaytime= 1.0,
						feedback = 0.0;
					var delay, fb;
					delay = BufDelayL.ar(buf, (In.ar(in) * inputamp) + (LocalIn.ar(1)), delaytime) * amp;
					LocalOut.ar(delay * feedback);
					Out.ar(out, (delay * amp));
				
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
	
	allocBufs { arg n=8, len=60.0, nch=1;
			^Buffer.allocConsecutive(n, this.s, this.s.sampleRate * len, nch);
	}

	recOnce { arg in, buf;
		^Synth.new(\rec, [\in, this.in_b.index + in, \buf, buf, \done, 2], this.ig, \addAfter);
	}
}