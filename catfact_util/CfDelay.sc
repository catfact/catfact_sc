// a bank of 4 delays with feedback.
CfDelay4 {
	var <>s;
	var <>del_buf, <>del_in_b, <>del_in_s;
	var <>del_s;
		
	*new {
		arg server, target;
		^super.new.init(server, target);
	}

	init {
		arg server, target;
		s = server;	
		if(target==nil, {target = s});
		
		Routine {
		
			SynthDef.new(\patch, {arg in=0, out=0, amp=1, amplag = 0.01;
				var sig;
				amp = Lag.kr(amp, amplag);
				sig = In.ar(in);
				Out.ar(out, sig * amp);
			}).send(s);
			
			s.sync;
							
			SynthDef.new(\bufd_svf, {
				arg in=0, out=0, buf=0, amp=1.0, amplag=0.1,
				delaytime=1.0,
				hz = 8000.0,
				rq=2.0,
				filtermix=1.0, filterlag=1.0,
				fb=0.0;
				
				var input, del, output, filter;
				
				amp = Lag.kr(amp, amplag);
				hz = Lag.kr(hz, filterlag);
				rq = Lag.kr(rq, filterlag);
				
				fb = fb.min(0.8);
				
				input = In.ar(in) + (LocalIn.ar(1) * fb);
				del = BufDelayL.ar(buf, input, delaytime);
				
				//filter = SVF.ar(del, hz, res, low, band, high, notch, peak);
				filter = BPF.ar(del, hz, rq);
				//output = del*(1.0 - filtermix) + filter*filtermix;
				output= del + (filter * filtermix);
				
				LocalOut.ar(output);
				Out.ar(out, output * amp);
			}).send(s);
			
			s.sync;			
			
			
			del_buf = Array.fill(4, { Buffer.alloc(s, 120.0 * s.sampleRate, 1); });
			
			0.1.wait;
			
			del_in_b = Bus.audio(s, 1);
			del_in_s = Synth.new(\patch, [\in, 0, \out, del_in_b.index], target);
			
			del_s = Array.fill(4, { arg i;
				Synth.new(\bufd_svf, [
				\in, del_in_b.index, \out, i % 2, \buf, del_buf[i].bufnum], del_in_s, \addAfter)
			});
			
			del_s.do({ arg syn, i;
				syn.set(\band, 0.0);
				syn.set(\amp, 1.0);
				syn.set(\fb, 0.5);
				syn.set(\delaytime, (i + 1) * 4.0); 
				syn.set(\res, 0.0);
			});
		
		}.play;	// init routine
	} // init func
}

// a bank of 4 delays with feedback, adc version.
CfAdcDelay4 {
	var <>s;
	var <>del_buf, <>del_in_b, <>del_in_s;
	var <>del_s;
		
	*new {
		arg server, target;
		^super.new.init(server, target);
	}

	init {
		arg server, target;
		s = server;	
		if(target==nil, {target = s});
		
		Routine {
		
			SynthDef.new(\adc_patch, {arg in=0, out=0, amp=1, amplag = 0.01;
				var sig;
				amp = Lag.kr(amp, amplag);
				sig = SoundIn.ar(in);
				Out.ar(out, sig * amp);
			}).send(s);
			
			s.sync;
							
			SynthDef.new(\bufd_svf, {
				arg in=0, out=0, buf=0, amp=1.0, amplag=0.1,
				delaytime=1.0,
				hz = 8000.0,
				rq=2.0,
				filtermix=1.0, filterlag=1.0,
				fb=0.0;
				
				var input, del, output, filter;
				
				amp = Lag.kr(amp, amplag);
				hz = Lag.kr(hz, filterlag);
				rq = Lag.kr(rq, filterlag);
				
				fb = fb.min(0.8);
				
				input = In.ar(in) + (LocalIn.ar(1) * fb);
				del = BufDelayL.ar(buf, input, delaytime);
				
				//filter = SVF.ar(del, hz, res, low, band, high, notch, peak);
				filter = BPF.ar(del, hz, rq);
				//output = del*(1.0 - filtermix) + filter*filtermix;
				output= del + (filter * filtermix);
				
				LocalOut.ar(output);
				Out.ar(out, output * amp);
			}).send(s);
			
			s.sync;			
			
			
			del_buf = Array.fill(4, { Buffer.alloc(s, 120.0 * s.sampleRate, 1); });
			
			0.1.wait;
			
			del_in_b = Bus.audio(s, 1);
			del_in_s = Synth.new(\adc_patch, [\in, 0, \out, del_in_b.index], target);
			
			del_s = Array.fill(4, { arg i;
				Synth.new(\bufd_svf, [
				\in, del_in_b.index, \out, i % 2, \buf, del_buf[i].bufnum], del_in_s, \addAfter)
			});
			
			del_s.do({ arg syn, i;
				syn.set(\band, 0.0);
				syn.set(\amp, 1.0);
				syn.set(\fb, 0.5);
				syn.set(\delaytime, (i + 1) * 4.0); 
				syn.set(\res, 0.0);
			});
		
		}.play;	// init routine
	} // init func
}