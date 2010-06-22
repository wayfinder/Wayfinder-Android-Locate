/*******************************************************************************
 * Copyright (c) 1999-2010, Vodafone Group Services
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright 
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above 
 *       copyright notice, this list of conditions and the following 
 *       disclaimer in the documentation and/or other materials provided 
 *       with the distribution.
 *     * Neither the name of Vodafone Group Services nor the names of its 
 *       contributors may be used to endorse or promote products derived 
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 ******************************************************************************/
package com.vodafone.locate.data;

import com.wayfinder.core.Core;
import com.wayfinder.core.favorite.Favorite;
import com.wayfinder.core.search.onelist.OneListSearchReply.SearchMatch;
import com.wayfinder.core.shared.Position;

/**
 * Currently this class is not used, since there is no point having a wrapper for the
 * {@link SearchMatch} data structure from {@link Core}. 
 */

public class Landmark {

	private SearchMatch match;
	private Favorite favorite;
	private int type;

	private static final int TYPE_SEARCH_MATCH = 0;
	private static final int TYPE_RECENT_DESTINATION = 1;// will be added later
	private static final int TYPE_FAVORITE = 2;

	private String matchName;
	private String matchAddress;

	public Landmark() {
		// only for demo!!
	}

	public Landmark(SearchMatch match) {
		this.match = match;
		this.type = TYPE_SEARCH_MATCH;
		tweakNameAddress();
	}

	public Landmark(Favorite favorite) {
		this.favorite = favorite;
		this.type = TYPE_FAVORITE;
	}

	public String getName() {
		if (type == TYPE_SEARCH_MATCH) {
			// return match.getMatchName();
			return matchName;
		} else {
			return favorite.getName();
		}
	}

	public String getDescription() {
		if (type == TYPE_SEARCH_MATCH) {
			// return match.getMatchLocation();
			return matchAddress;
		} else {
			return favorite.getDescription();
		}
	}

	public String getCategoryImageName() {
		if (type == TYPE_SEARCH_MATCH) {
			return match.getMatchCategoryImageName();
		} else {
			return favorite.getIconName();
		}
	}

	public String getBrandImageName() {
		if (type == TYPE_SEARCH_MATCH) {
			return match.getMatchBrandImageName();
		} else {
			return favorite.getIconName();
		}
	}

	public Position getPosition() {
		if (type == TYPE_SEARCH_MATCH) {
			return match.getPosition();
		} else {
			return favorite.getPosition();
		}
	}

	public String getId() {
		if (type == TYPE_SEARCH_MATCH) {
			return match.getMatchID();
		} else {
			return "";
		}
	}

	public long getTimestamp() {
		return 0;
	}

	public long getPlaceDBId() {
		return 0;
	}

	public String getProviderImageName() {
		if (type == TYPE_SEARCH_MATCH) {
			return match.getMatchProviderImageName();
		} else {
			return "";
		}
	}

	public Favorite getFavorite() {
		return favorite;
	}

	private void tweakNameAddress() {
		int i = match.getMatchName().indexOf(',');
		if (i != -1) {
			this.matchName = match.getMatchName().substring(0, i).trim();
			this.matchAddress = match.getMatchName().substring(i + 1).trim();
		} else {
			this.matchName = match.getMatchName();
			this.matchAddress = match.getMatchLocation();
		}
	}

}
