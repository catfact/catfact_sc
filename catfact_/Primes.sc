// some useful static methods 
Primes {
	// "geometric wrap" : given a value and a factor,
	// coerce the value to the range [1, factor]
	// by multiplying/dividing by the factor
	// (optionally, apply only the upper bound)
	*geoWrap {
		arg x, factor, lowPower=0;
		var lower = factor ** lowPower;
		while( {x > factor}, {
			x  = x / factor;
		});
		while( {x < lower}, {
			x = x * factor;
		});
		^x
	}

	*gcd {
		arg a, b;
		var tmp;
		while( {b!=0}, {
			tmp = b;
			b = a % b;
			a = tmp;
		});
		^a;
	}
}

// generate sequence of primes from quadractic polynomial
QuadPrimes {
	var <>a, <>b, <>c, <>i;
	var <>primes;
	
	*new { arg a_, b_, c_;
		^super.new.init(a_, b_, c_);
	}
	
	init { arg a_, b_, c_;
		this.set(a_, b_, c_);
		i = 0;
		primes = List.new;
	}

	set {
		arg a_, b_, c_;
		a=a_; b=b_; c=c_;
	}

	// get the next prime produced by this quadratic sequence
	next {
		var y;
		var res;
		y = a*(i*i) + b*i + c;
		i = i +1;
		if(y.isPrime, {
			primes.add(y);
			res = true;
		}, {
			res = false;
		});
		^res
	}


	// generate a set of N primes
	generate {
		arg n=24, wrap=8.0, lowPower=0, sort=true;
		// count iterations to avoid infinite loops...
		var i=0, max=0x0fffffff;
		var r;
		primes.clear;
		while( { (primes.size < n) && (i < max) }, {
			this.next;
			i = i+1;
		});
		r = primes.collect({ |x| Primes.geoWrap(x, wrap, lowPower) });
		if(sort, { r = r.sort; });
		^r
	}
}

// basic "prime spiral" generator
RatioSpiral  {
	// current value
	var <>value;
	// list of primes
	var <primes;
	// current index into prime list
	var <>idx;
	
	// advance to the next value in the spiral
	advance {
		idx = (idx+1) % (primes.size);
		value = Primes.geoWrap(value * primes[idx]);
		value
	}

	// set the list of primes
	setPrimeList {
		arg primes_;
		primes = primes_.asList;
		idx = idx % (primes.size);
	}

}
