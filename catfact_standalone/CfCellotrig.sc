CfCellotrig {

	var <>server;
	
	var <>onset_func;
	var <>trig_func;
	
	var <>onset_fft_buf;
	var <>onset_s;
	var <num_trigs;
	var <>trig_s;
	var <trig_hz;
	
	var <>responder;
	
	*new {
		arg server, num_trigs_arg=4;
		^super.new.init(server, num_trigs_arg);
	}
	
	init {
		arg server_arg, num_trigs_arg;
		server = server_arg;
		num_trigs = num_trigs_arg;
		
		Routine {
			server.waitForBoot {			
				
				
				SynthDef.new(\targetNoteEvents, {
					arg in= 0,
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
				}).load(server);
				
				SynthDef.new(\onsetEvents, {
					arg in =		0,
						buf =	0,
						trigid =	0,
						thresh =	0.7,
						relax =	2.0;
				
					var sig, chain, onsets;
					
					sig = SoundIn.ar(in);
					chain = FFT(buf, sig);
					onsets = Onsets
					.kr(chain, thresh, \rcomplex, relax);
					SendTrig.kr(onsets, trigid);	
				}).load(server);
				
				postln("defined synthdefs");
				
				0.1.wait;
				
				onset_fft_buf = Buffer.alloc(server, 1024, 1);
				postln("allocated fft buf");
				
				
				0.1.wait;
				
				onset_s = Synth.new(\onsetEvents, [\buf, onset_fft_buf, \trigid, 100], server);
				
				//trig_hz = Array.fill(num_trigs, { arg i; 110 * (2 ** i); });
				trig_hz = Array.with(81, 36, 38, 69, 43, 64).midicps;
				trig_s = trig_hz.collect({
					arg hz, i;
					Synth.new(\targetNoteEvents, [
						\in_hz, hz, 
						\hz_tolerance, (hz.cpsmidi + 0.5).midicps - hz,
						\hist_len, 1.0,
						\trig_id, i
					], server);
				});
				
				onset_func = {
					arg t;
					postln("onset at " ++ t ++ " s");
				};
				
				trig_func = Array.fill(num_trigs, { 
					{	
						arg t, which;
						postln("trigger " ++ which);
					};
				});
				
				responder = OSCresponderNode(nil, '/tr', { 
					arg t, r, msg;
					if (msg[2] == 100, {
						onset_func.value(t);
					}, {
						trig_func.value(t, msg[2]);
					});
					
				}).add;
				
				postln("added osc responder");
								
				postln("init done");
				
			} // waitforboot
		}.play;	// init routine
	}
				
}