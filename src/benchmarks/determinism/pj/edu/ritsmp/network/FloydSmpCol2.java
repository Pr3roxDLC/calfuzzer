//******************************************************************************
//
// File:    FloydSmpCol2.java
// Package: benchmarks.determinism.pj.edu.ritsmp.network
// Unit:    Class benchmarks.determinism.pj.edu.ritsmp.network.FloydSmpCol2
//
// This Java source file is copyright (C) 2008 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the Parallel Java Library ("PJ"). PJ is free
// software; you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// PJ is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

package benchmarks.determinism.pj.edu.ritsmp.network;

//import benchmarks.determinism.pj.edu.ritpj.Comm;
import benchmarks.determinism.pj.edu.ritpj.IntegerForLoop;
import benchmarks.determinism.pj.edu.ritpj.ParallelRegion;
import benchmarks.determinism.pj.edu.ritpj.ParallelTeam;

import benchmarks.determinism.pj.edu.ritutil.Random;

import static edu.berkeley.cs.detcheck.Determinism.openDeterministicBlock;
import static edu.berkeley.cs.detcheck.Determinism.closeDeterministicBlock;
import static edu.berkeley.cs.detcheck.Determinism.requireDeterministic;
import static edu.berkeley.cs.detcheck.Determinism.assertDeterministic;
import static edu.berkeley.cs.detcheck.Predicate.ApproxEquals;

public class FloydSmpCol2
	{

// Prevent construction.

	private FloydSmpCol2()
		{
		}

// Shared variables.

	// Number of nodes.
	static int n;

	// Distance matrix.
	static double[][] d;

// Main program.

	/**
	 * Main program.
	 */
	public static void main
		(String[] args)
		throws Exception
		{
		// Start timing.
		long t1 = System.currentTimeMillis();

		// Parse command line arguments.
		if (args.length != 3) usage();
		long seed = Long.parseLong (args[0]);
		double radius = Double.parseDouble (args[1]);
		n = Integer.parseInt (args[2]);

                openDeterministicBlock();
                requireDeterministic(seed);
                requireDeterministic(radius);
                requireDeterministic(n);

		// Set up pseudorandom number generator.
		Random prng = Random.getInstance (seed);

		// Generate random node locations in the unit square.
		double[] x = new double [n];
		double[] y = new double [n];
		for (int i = 0; i < n; ++ i)
			{
			x[i] = prng.nextDouble();
			y[i] = prng.nextDouble();
			}

		// Compute distance matrix elements.
		d = new double [n] [n];
		for (int r = 0; r < n; ++ r)
			{
			double[] d_r = d[r];
			for (int c = 0; c < n; ++ c)
				{
				double dx = x[r] - x[c];
				double dy = y[r] - y[c];
				double distance = Math.sqrt (dx*dx + dy*dy);
				d_r[c] =
					(distance <= radius ?
						distance :
						Double.POSITIVE_INFINITY);
				}
			}

		// Run Floyd's Algorithm.
		//     for i = 0 to N-1
		//         for r = 0 to N-1
		//             parallel for c = 0 to N-1
		//                 D[r,c] = min (D[r,c], D[r,i] + D[i,c])
		long t2 = System.currentTimeMillis();
		new ParallelTeam().execute (new ParallelRegion()
			{
			public void run() throws Exception
				{
				for (int ii = 0; ii < n; ++ ii)
					{
					final int i = ii;
					final double[] d_i = d[i];
					for (int r = 0; r < n; ++ r)
						{
						final double[] d_r = d[r];
						execute (0, n-1, new IntegerForLoop()
							{
							public void run (int first, int last)
								{
								for (int c = first; c <= last; ++ c)
									{
									d_r[c] = Math.min (d_r[c], d_r[i] + d_i[c]);
									}
								}
							});
						}
					}
				}
			});
		long t3 = System.currentTimeMillis();

                // System.out.println(Arrays.deepToString(d));
                assertDeterministic(d, new ApproxEquals(1e-10));
                closeDeterministicBlock();

		// Stop timing.
		long t4 = System.currentTimeMillis();
		System.out.println ((t2-t1) + " msec gen");
		System.out.println ((t3-t2) + " msec calc");
		System.out.println ((t4-t3) + " msec post");
		System.out.println ((t4-t1) + " msec total");
		}

// Hidden operations.

	private static void usage()
		{
		System.err.println ("Usage: java -Dpj.nt=<K> benchmarks.determinism.pj.edu.ritsmp.network.FloydSmpCol2 <seed> <radius> <N>");
		System.err.println ("<K> = Number of parallel threads");
		System.err.println ("<seed> = Random seed");
		System.err.println ("<radius> = Node adjacency radius");
		System.err.println ("<N> = Number of nodes");
		System.exit (1);
		}

	}
