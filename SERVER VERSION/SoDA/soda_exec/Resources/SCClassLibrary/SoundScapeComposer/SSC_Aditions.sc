+ Cartesian {
	
	rand{
		var newX, newY, newZ;
		newX = x.rand;
		newY = y.rand;
		newZ = z.rand;
		^Cartesian(newX, newY, newZ);
	}

	rrand{ arg cartesian; // this will return a random cartesian in the range of the current and provided one

		var newX, newY, newZ;
		newX = rrand(cartesian.x, x);
		newY = rrand(cartesian.y, y);
		newZ = rrand(cartesian.z, z);
		^Cartesian(newX, newY, newZ);
	}

	isWithinArea{ arg locus, bounds; // asks whether a locus (Should be a Cartesian) is within the zone defined by this and bounds
		if (
			((locus.x>=x)&&(locus.x<=(x + bounds.x)))&&
			((locus.y>=y)&&(locus.y<=(y + bounds.y)))&&
			((locus.z>=z)&&(locus.z<=(z + bounds.z)))
		) {^true} {^false}
	}
	
}
