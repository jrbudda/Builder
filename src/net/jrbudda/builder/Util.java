package net.jrbudda.builder;

import java.util.ArrayList;
import java.util.List;

public class Util {

	public static List<BuildBlock> spiralPrintLayer(int y, BuildBlock[][][] a)
	{
		int i, k = 0, l = 0;

		int m = a.length;
		int n = a[0][0].length;

		List<BuildBlock> out = new ArrayList<BuildBlock>();

		/*  k - starting row index
	        m - ending row index
	        l - starting column index
	        n - ending column index
	        i - iterator
		 */

		while (k < m && l < n)
		{
			/* Print the first row from the remaining rows */
			for (i = l; i < n; ++i)
			{
				out.add(a[k][y][i]);
			}
			k++;

			/* Print the last column from the remaining columns */
			for (i = k; i < m; ++i)
			{
				out.add(a[i][y][n-1]);
			}
			n--;

			/* Print the last row from the remaining rows */
			if ( k < m)
			{
				for (i = n-1; i >= l; --i)
				{
					out.add(a[m-1][y][i]);
				}
				m--;
			}

			/* Print the first column from the remaining columns */
			if (l < n)
			{
				for (i = m-1; i >= k; --i)
				{
					out.add(a[i][y][l]);
				}
				l++;   
			}       
		}

		java.util.Collections.reverse(out);

		return out;
	}



	public static List<BuildBlock> LinearPrintLayer(int y, BuildBlock[][][] a)
	{
		int i = 0,k = 0;
		int di = 1;
		int dk=1;

		int m = a.length;
		int n = a[0][0].length;

		List<BuildBlock> out = new ArrayList<BuildBlock>();

		/*  k - starting row index
	        m - ending row index
	        l - starting column index
	        n - ending column index
	        i - iterator
		 */

		do{

			out.add(a[i][y][k]);
			i+=di;
			if(i >=m || i < 0) {
				di*=-1;
				i+=di;
				k+=dk;
				if (k >= n||k<0) {
					k +=1;		
					if(k>=n) break;
				}		
			}


		}while(true);

		return out;
	}



}
