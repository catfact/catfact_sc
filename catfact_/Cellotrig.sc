CfCellotrig {

	var <>server;
	
	var <>onset_func;
	var <>trig_func;
	
	var <>onset_fft_buf;
	var <>onset_s;
	var <target_notes;
	var <>trig_s;
	var <trig_hz;
	
	var <>responder;
	
	*new {
		arg server, target_notes_arg;
		^super.new.init(server, target_notes_arg);
	}
	
	init {
		arg server_arg, target_notes_arg;
		server = server_arg;
		target_notes = target_notes_arg;
		
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
						inpitchrange,
						tgt_hz;
						
					# in_hz, in_hz_flag = Pitch.kr(SoundIn.ar(in));
					tgt_hz = target_hz;
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
					onsets = Onsets.kr(chain, thresh, \rcomplex, relax);
					SendTrig.kr(onsets, trigid);	
				}).load(server);
				
				postln("defined synthdefs");
				
				0.1.wait;
				
				onset_fft_buf = Buffer.alloc(server, 1024, 1);
				postln("allocated fft buf");
				
				
				0.1.wait;
				
				onset_s = Synth.new(\onsetEvents, [\buf, onset_fft_buf, \trigid, 100], server);
				
				trig_hz = target_notes.midicps;
				trig_s = trig_hz.collect({
					arg hz, i;
					Synth.new(\targetNoteEvents, [
						\target_hz, hz, 
						\hz_tolerance, (hz.cpsmidi + 0.5).midicps - hz,
						\hist_len, 1.0,
						\trig_id, i
					], server);
				});
				
				onset_func = {
					arg t;
					postln("onset at " ++ t ++ " s");
				};
				
				trig_func = Array.fill(target_notes.size, { 
					{	
						arg t, which;
						postln("trigger " ++ which);
					};
				});
				
				responder = OSCresponderNode(nil, '/tr', { 
					arg t, r, msg;
					
					// [t, r, msg].postln;
					
					if (msg[2] == 100, {
						onset_func.value(t);
					}, {
						// "note event...".postln;
						// msg[2].postln;
						trig_func[msg[2]].value(t, msg[2]);
					});
					
					
				}).add;
				
				// postln("added osc responder");
				// postln("init done");
				
			} // waitforboot
		}.play;	// init routine
	}
				
}