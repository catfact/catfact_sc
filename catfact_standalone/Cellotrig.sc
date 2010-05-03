CfCellotrg {

	var <>s;
	var <>onset_func;
	var <>trigger_func;
	
	var <>onset_fft_buf;
	var <>onset_s;
	var <>trigger_s;
	
	var <>onset_r;
	var <>trigger_r;
	
	*new {
		arg server;
		^super.new.init(server);
	}
	
	init {
		arg server;
		s = server;
		
		Routine {
			s.waitForBoot {			
				
				
				SynthDef.new(\targetNoteEvents, {
					arg	in= 0,
						out= 0,
						outswitch=0,
						hist_len= 0.5,
						hist_thresh=0.9,
						target_hz= 220,
						hz_tolerance = 10,
						trig_id=0;
						
					var in_hz, in_hz_flag,
						hist_min = 0.001,
						sweep, gate, osc, 
						sweep_trig, gate_trig, osc_trig,
						inpitchrange;
						
					# in_hz, in_hz_flag = Tartini.kr(SoundIn.ar(in), 0.93,1024,512,512);
					inpitchrange = ((in_hz - target_hz).abs < hz_tolerance);
					gate = inpitchrange;
					sweep_trig = inpitchrange;
					sweep = Sweep.kr(sweep_trig, hist_thresh / hist_len);
					osc_trig = ((gate * sweep) > hist_thresh);
					Out.kr(out, outswitch * [in_hz, inpitchrange, gate, sweep, osc_trig] );
					SendTrig.kr(osc_trig, trig_id);
				}).store;
				
				SynthDef.new(\onsetEvents, {
					arg in =		0,
						buf =	0,
						trigid =	0,
						thresh =	0.7,
						relax =	2.0;
				
					var sig, chain, onsets;
					
					sig = SoundIn.ar(in);
					chain = FFT(buf, sig);
					onsets = Onsets.kr(chain, thresh, \rcomplex, relax);
					SendTrig.kr(onsets, trigid);	
				}).store;
				
				postln("defined synthdefs");
				
				0.1.wait;
				
				onset_fft_buf = Buffer.alloc(s, 1024, 1);
				postln("allocated fft buf");
				
				
				0.1.wait;
				
				
				onset_s = Synth.new(\onsetEvents, [\buf, onset_fft_buf, \trig_id, 0], s);
				trigger_s = Synth.new(\targetNoteEvents, [
					\in_hz, 220, 
					\hz_tolerance, 5.0,
					\hist_len, 1.0,
					\trig_id, 1
				], s);
				
				onset_func = {
					arg t, r, msg;
					[t, r, msg].postln;
				};
				
				trigger_func = {
					arg t, r, msg;
					[t, r, msg].postln;
				};
				
				onset_r = OSCresponderNode(nil, '/tr', {
					arg t, r, msg;
					[t, r, msg].postln;
					//onset_func
				}).add;
				trigger_r = OSCresponderNode(nil, '/tr', trigger_func).add;
								
				
			} // waitforboot
		}.play;	// init routine
	}
				
}