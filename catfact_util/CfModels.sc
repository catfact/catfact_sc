// CfModels.sc
// a collection of iterated numerical structures



// abstract interface class
CfModel2d {
	var <>n, <>val, <>function;
	
	*new { arg n;
		^super.new.cfModel2dInit(n);
	}
	
	cfModel2dInit { arg argN;
		n = argN;
		val = Array.fill(n, {0.0});
		function = {};
	}
	
	iterate {
		/*
		n.do({
			// something... define in subclasses
		});
		*/
		function.value(val);
	}
}

// fermi-pasta-ulam nonlinear string model
// wrapped in a torus
CfFpuRing : CfModel2d{	
	var <>n; 		// number of weights
	var <>a; 		// nonlinearity parameter 
	var <>dt; 	// time step of simulation
	var <>k;		// force constant
	var <>d;		// damping coefficient
	var <>wrap; 	// wrap positions?
	var <>clip; 	// clip positions?
	var <>vel, <>acc; // current position, velocity, acceleration for all weights
	
	*new { arg n;
		^super.new(n).cfFpuRingInit;	
	}

	cfFpuRingInit {
		a = 2.4;
		dt = 0.02;
		k = 1.0;
		d = 0.975;
		clip = false;
		wrap = true;
		vel = Array.fill(n, {0.0});
		acc = Array.fill(n, {0.0});
	}
	
	iterate {
		n.do({
			arg i;
			var dist;
	
			// alternately:     F(x) = -k x + b x3 
			// displacement distance:
			dist = val[(i+1).wrap(0, n-1)] - val[i];
			dist = dist + val[(i-1).wrap(0, n-1)] - val[i];
			
			// [i, dist, val[i], acc[i], vel[i]].postln;
			
			acc[i] = -1* k * dist + (a * dist * dist * dist);
			
			// update velocity from acceleration
			vel[i] = vel[i] + (acc[i] * dt);
			// damping
			vel[i] = vel[i] * d;
					
			// update position from velocity
			val[i] = val[i] + (vel[i] * dt);
			if (wrap, {val[i] = val[i].wrap(-1.0, 1.0); });
			if (clip, {val[i] = val[i].clip(-1.0, 1.0); });
		});
		super.iterate;
	}
}

// cellular automaton, binary rules
CfBinaryAut : CfModel2d{
	var <>rule;

	*new { arg n;
		^super.new(n).cfBinaryAutInit(n);	
	}

	cfBinaryAutInit {
		val.postln;
	}
	
	iterate {
		val.do({ arg v, i;
			var code = 0; // 8 bit input to rule
			code = code | (val[(i-1).wrap(0, n-1)] << 2);
			code = code | (v << 1);
			code = code | (val[(i+1).wrap(0, n-1)]);
			val[i] = (rule & (1 << code)) >> code;
		});
		super.iterate;
	}	
}

// cellular automaton, continuous rules
CfContinuousAut : CfModel2d{
	var <>weights_l, <>weights_r; // weighting functions for the left and right, 
	var <>weight_res; // resolution of the weighting
	var <>wrap, <>clip;
	
	*new { arg n;
		^super.new(n).cfContinuousAutInit(n);	
	}

	cfContinuousAutInit {
		weight_res = 128;
		weights_l = Array.fill(weight_res, {0.5});
		weights_r = Array.fill(weight_res, {0.5});
		wrap = true;
		clip = false;
		val.postln;
	}
	
	iterate {
		var xR, xL, iR, iL, yR, yL;
		val.do({ arg v, i;
			// use neighbor values in lookup table of weights
			xR = val[(i+1).wrap(0, n-1)];
			iR = (xR * (weight_res-1));
			//("iR: "++iR).postln;
			/*
			yR = weights_r[iR.floor] +
				(weights_r[(iR.floor + 1).wrap(0, weight_res-1)] - weights_r[iR.floor]) *
				(iR - iR.floor);
			*/
			yR = weights_r[iR.floor.wrap(0, weight_res-1)];
				
			xL = val[(i-1).wrap(0, n-1)]; 
			iL = (xL * (weight_res-1));
			//("iL: "++iL).postln; 
			/*
			yL = weights_l[iL.floor] +
				(weights_l[(iL.floor + 1).wrap(0, weight_res-1)] - weights_l[iL.floor]) *
				(iL - iL.floor);
			*/
			yL = weights_l[iL.floor.wrap(0, weight_res-1)];
			
			
			// DEBUG
			// [iR, iL].postln;
			
			val[i] = v + (yL + yR)*0.5;
			
			if (wrap, {val[i] = val[i].wrap(-1.0, 1.0); });
			if (clip, {val[i] = val[i].clip(-1.0, 1.0); });
			
			//val.postln;
		});
		super.iterate;
	}
}