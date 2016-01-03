import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Siteswap {
	private int numHands;
	private String type;
	private List<Beat> beats;
	private boolean hasInDegree;
	private Integer largestTossHeight;

	public Siteswap(int numHands, String type) {
		this.numHands = numHands;
		this.beats = new ArrayList<Beat>();
		this.type = type;
		this.hasInDegree = false;
	}

	private Siteswap(List<Beat> beats, int numHands, String type) {
		this.beats = beats;
		this.numHands = numHands;
		this.type = type;
		this.hasInDegree = false;
	}

	public int numHands() {
		return this.numHands;
	}

	public String type() {
		return this.type;
	}

	public boolean hasInDegree() {
		return this.hasInDegree;
	}

	public double numBalls() {
		int total = 0;
		int numInfinities = 0;
		for(Beat b : beats) {
			total += b.totalFiniteBeatValue();
			numInfinities += b.numInfiniteTosses();
		}
		if(numInfinities > 0) {
			return Double.POSITIVE_INFINITY;
		} else if(numInfinities < 0) {
			return Double.NEGATIVE_INFINITY;
		} else {
			return (double)total/(double)beats.size();
		}
	}

	public int period() {
		return this.beats.size();
	}

	private void calculateInDegree() {
		//calculate the indegree of each node (beat.hand)
		//(better than calculating it as the siteswap is created, because
		//a given node might not exist when adding a node that throws to it)

		//reset all indegrees, since we don't want to increment them if they don't start at zero
		for(int b=0; b<period(); b++) {
			for(int h=0; h<getBeat(b).numHands(); h++) {
				getBeat(b).getHand(h).inDegree = 0;
			}
		}

		int height;
		int destHand;
		int toBeat;
		for(int b=0; b<period(); b++) {
			for(int h=0; h<getBeat(b).numHands(); h++) {
				for(int t=0; t<getBeat(b).getHand(h).numTosses(); t++) {
					height = getBeat(b).getHand(h).getToss(t).height;
					destHand = getBeat(b).getHand(h).getToss(t).destHand;
					toBeat = (b + height) % period();
					if(toBeat < 0) {
						toBeat += period();
					}
					getBeat(toBeat).getHand(destHand).inDegree++;
				}
			}
		}
		hasInDegree = true;
	}

	public int getLargestTossHeight() {
		//at some point change this so it automatically updates whenever you add a toss (or a beat, or just alter the pattern in any way)
		for(Beat b : beats) {
			for(Beat.Hand h : b.hands) {
				for(Beat.Hand.Toss t : h.tosses) {
					if(largestTossHeight == null || t.height > largestTossHeight) {
						largestTossHeight = t.height;
					}
				}
			}
		}
		return largestTossHeight;
	}

	public boolean isValid() {
		//after changing how the class works by making there never be empty anything, just zero-valued things,
		//I'm afraid I might have messed up hasIndegree
		//if(!hasInDegree) {
			calculateInDegree();
		//}
		//see if each node's indegree equals its outdegree 
		for(int b=0; b<period(); b++) {
			for(int h=0; h<getBeat(b).numHands(); h++) {
				if(getBeat(b).getHand(h).inDegree != getBeat(b).getHand(h).numTosses()) {
					return false;
					//inDegree calculation factors in throws of height zero, so we can count them in numTosses as well
				}
			}
		}
		return true;
	}

	public void addBeat(Beat newBeat) {
		beats.add(newBeat);
		hasInDegree = false;
	}

	public void addZeroBeat() {
		beats.add(new Beat(period()));
	}

	//adds a new toss from the given hand at the given beat to the given desthand with the given height
	public boolean addToss(int atBeat, int atHand, int tossHeight, boolean isInfinite, int destHand, boolean isAntiToss) {
		if(atHand > this.numHands || destHand > this.numHands) {
			return false;
		} else {
			if(atBeat >= 0) {
				while(atBeat >= period()) {
					this.addZeroBeat();
				}
				this.getBeat(atBeat).getHand(atHand).addToss(tossHeight, isInfinite, destHand, isAntiToss);
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean addToss(int atBeat, int atHand, int tossHeight, int destHand) {
		return addToss(atBeat, atHand, tossHeight, false, destHand, false);
	}

	public boolean strictAddToss(int atBeat, int atHand, int tossHeight, boolean isInfinite, int destHand, boolean isAntiToss) {
		if(atBeat >= 0) {
			this.addToss(atBeat, atHand, tossHeight, isInfinite, destHand, isAntiToss);
			return true;
		} else {
			// shift everything forward by b beats, putting zero-valued beats in the front
			int shiftAmount = -atBeat + 1;
			for(Beat beat : beats) {
				beat.beatIndex += shiftAmount;
				for(Beat.Hand hand : beat.hands) {
					hand.beatIndex += shiftAmount;
				}
			}
			for(int i=9; i<=shiftAmount; i++) {
				beats.add(0, new Beat(shiftAmount - i));
			}
			this.getBeat(0).getHand(atHand).addToss(tossHeight, isInfinite, destHand, isAntiToss);
			return true;
		}
	}

	public boolean strictAddToss(int atBeat, int atHand, int tossHeight, boolean isInfinite, int destHand) {
		return strictAddToss(atBeat, atHand, tossHeight, isInfinite, destHand, false);
	}

	public boolean strictAddToss(int atBeat, int atHand, int tossHeight, int destHand) {
		return strictAddToss(atBeat, atHand, tossHeight, false, destHand, false);
	}

	public Beat getBeat(int b) {
		b = b % period();
		if(b < 0) b += period();
		return beats.get(b);
	}

	public Beat getLastBeat() {
		if(beats.size() < 1) {
			return null;
		} else {
			return beats.get(beats.size() - 1);
		}
	}

	public Siteswap deepCopy() {
		return getCopyOfSubPattern(0, period());
	}

	public Siteswap getCopyOfSubPattern(int startBeat, int endBeat) {
		//get deep copy of each beat within specified indices
		List<Beat> newBeats = new ArrayList<Beat>();
		for(int b=startBeat; b<=endBeat; b++) {
			newBeats.add(beats.get(b).deepCopy());	
		}
		return new Siteswap(newBeats, numHands, type);
	}

	public void antiTossify() {
		Beat.Hand curHand;
		Beat.Hand.Toss curToss;
		for(int b=0; b<period(); b++) {
			for(int h=0; h<numHands(); h++) {
				curHand = getBeat(b).getHand(h);
				for(int t=0; t<curHand.numTosses(); t++) {
					curToss = curHand.getToss(t);
					//check if its height is negative
					if(curToss.height() < 0) {
						//first make it an antitoss (make its height positive, set antitoss flag)
						curToss.makeAntiToss(true); //(this also negates its height)
						//then shift the toss back in time through the siteswap according to its height
						//...as long as it isn't infinite
						//(if it is, then we certainly can't shift it back)
						//(we just leave it where it is, it works out with getState in the end, don't worry)
						if(!curToss.isInfinite()) {
							//shift curtoss back in time by its height value
							//first remove it from where it was
							curHand.removeToss(t);
							//then add it where it needs to go
							int destBeat = (b - curToss.height()) % period();
							if(destBeat < 0) {
								destBeat += period();
							}
							getBeat(destBeat).getHand(h).addToss(curToss);
						}
					}
				}
			}
		}
	}

	public void unAntiTossify() {
		Beat.Hand curHand;
		Beat.Hand.Toss curToss;
		for(int b=0; b<period(); b++) {
			for(int h=0; h<numHands(); h++) {
				curHand = getBeat(b).getHand(h);
				for(int t=0; t<curHand.numTosses(); t++) {
					curToss = curHand.getToss(t);
					//check if it's an antitoss
					if(curToss.isAntiToss()) {
						//shift the toss forward in time through the siteswap according to its height
						//...as long as it isn't infinite
						//(if it is, then we certainly can't shift it back)
						//(we just leave it where it is, it works out with getState in the end, don't worry)
						if(!curToss.isInfinite()) {
							//first, make it a regular toss (this also negates its height)
							curToss.makeAntiToss(false);
							//shift curtoss back in time by its height value
							//first remove it from where it was
							curHand.removeToss(t);
							//then add it where it needs to go
							int destBeat = (b - curToss.height()) % period();
							getBeat(destBeat).getHand(h).addToss(curToss);
						}
					}
				}
			}
		}
	}

	public Siteswap annexPattern(Siteswap toAnnex) {
		for(int b=0; b<toAnnex.period(); b++) {
			addBeat(toAnnex.getBeat(b));
		}
		return this;
	}

	public void removeLastBeat() {
		beats.remove(beats.size() - 1);
	}

	public void addStar() {
		//this operation only makes sense on two-handed siteswaps
		if(numHands != 2) {
			System.out.println("star notation only makes sense for two-handed patterns");
			return;
		}
		//save old period
		int oldPeriod = period();
		//add flipped versions of old beats to end of pattern
		for(int b=0; b<oldPeriod; b++) {
			addBeat(getBeat(b).starBeat(b+oldPeriod));
		}
	}

	public Siteswap sprungify() {
		return null;
		//later...
	}

	public String toString() {
		return beats.toString();
	}

	protected class Beat {
		private List<Hand> hands;
		private int beatIndex;

		private Beat(int beatIndex) {
			hands = new ArrayList<Hand>();
			this.beatIndex = beatIndex;
			for(int i=0; i<numHands; i++) {
				addZeroHand(i);
			}
		}

		private Beat(List<Hand> handList, int beatIndex) {
			hands = handList;
			this.beatIndex = beatIndex;
		}

		private void addZeroHand(int handIndex) {
			hands.add(new Hand(handIndex, beatIndex));
		}

		public int totalFiniteBeatValue() {
			int total = 0;
			for(Hand s : hands) {
				total += s.totalFiniteHandValue();
			}
			return total;
		}

		public int numInfiniteTosses() {
			int num = 0;
			for(Hand s : hands) {
				num += s.numInfiniteTosses();
			}
			return num;
		}

		public int numHands() {
			return hands.size();
		}

		public boolean isZeroBeat() {
			for(Hand h : hands) {
				if(!h.isZeroHand()) {
					return false;
				}
			}
			return true;
		}

		public Hand getHand(int index) {
			return hands.get(index);
		}

		private Beat starBeat(int newBeatIndex) {
			if(numHands != 2) {
				//this should never happen, b/c the parent method checks for it...
				return this;
			}
			//since there is no addHand() method (because that wouldn't make sense in any situation where you aren't just creating a new beat)
			Beat newBeat = new Beat(newBeatIndex);
			List<Hand> newHandList = new ArrayList<Hand>();
			//add old right hand as new left hand, and vice-versa
			newHandList.add(hands.get(1).starHand(newBeatIndex));
			newHandList.add(hands.get(0).starHand(newBeatIndex));
			return new Beat(newHandList, newBeatIndex);
		}

		private Beat deepCopy() {
			//get deep copy of each hand in hands
			List<Hand> newHands = new ArrayList<Hand>();
			for(int h=0; h<hands.size(); h++) {
				newHands.add(hands.get(h).deepCopy());
			}
			return new Beat(newHands, beatIndex);
		}

		public String toString() {
			return hands.toString();
		}

		protected class Hand {
			protected List<Toss> tosses;
			protected int handIndex;
			private int beatIndex;
			private boolean isEmpty;
			private int inDegree;

			public Hand(int handIndex, int beatIndex) {
				this.tosses = new ArrayList<Toss>();
				this.isEmpty = true;
				this.handIndex = handIndex;
				this.beatIndex = beatIndex;
				this.inDegree = 0;
				addToss();
			}

			private Hand(List<Toss> newTosses, int newHandIndex, int newBeatIndex, boolean newIsEmpty) {
				this.tosses = newTosses;
				this.handIndex = newHandIndex;
				this.beatIndex = newBeatIndex;
				this.isEmpty = newIsEmpty;
				this.inDegree = 0;
			}

			public int totalFiniteHandValue() {
				int total = 0;
				Set<Toss> doneTosses = new HashSet<Toss>(); //this is for a weird thing described below
				for(Toss t : tosses) {
					if(!t.isInfinite()) {
						total += t.height;
					} else {
						/*this is a weird thing I realized.
						  in order to get the correct answer for numBalls,
						  you have to convert each positive-/negative-infinite-valued
						  toss pair to a finite-valued toss.
						  e.g. 1 & 0 [-& 1] becomes
						         \____/^    >> height = 2
						       1 2 0 1
						       so its numballs can be correctly calculated as
						       (1+2+0+1)/(1+1+1+1) = 4/4 = 1
						       (the number of balls needed at the beginning of the pattern)
						 */
						//so for this toss (t), whose value is &, we need to find
						//a toss whose value is -&
						//and we need to keep track of which infinite-valued tosses
						//we've already taken care of (so that's what doneTosses is for)
						//first we need to check that we haven't already taken care of this toss
						if(doneTosses.contains(t)) {
							continue;
						}
						//if not, then loop through beats to find a sister-toss
						for(int b=beatIndex; b<beatIndex + period(); b++) {
							int curBeatIndex = b % period();
							//check if current beat has a toss of value -&
							//so loop through hands
							for(int h=0; h<numHands(); h++) {
								//and loop through tosses
								for(int tossIndex=0; tossIndex<getBeat(curBeatIndex).getHand(h).numTosses(); tossIndex++) {
									//check if current toss has value -&
									Toss curToss = getBeat(curBeatIndex).getHand(h).getToss(tossIndex);
									if(curToss.isInfinite() && curToss.height < 0) {
										//then treat this as the &-throw we started with
										//being caught in this beat
										//thus it has height = b - beatIndex
										total += b - beatIndex;
										//and we can add both it and the original &-toss
										//to the done set
										doneTosses.add(curToss);
										doneTosses.add(t);
									}
								}
							}
						}
					}
				}
				return total;
			}

			public int numInfiniteTosses() {
				int num = 0;
				for(Toss t : tosses) {
					if(t.isInfinite()) {
						if(t.height() < 0) {
							num--;
						} else if(t.height() > 0) {
							num++;
						}
					}
				}
				return num;
			}

			public int normalizedNumTosses() {
				//makes antitosses count negatively towards toss total
				int out = 0;
				for(Toss t : tosses) {
					if(t.isAntiToss()) {
						out--;
					} else {
						out++;
					}
				}
				return out;
			}

			//add (height, destHand)!
			public void addToss(int height, int destHand) {
				addToss(height, false, destHand);
			}

			//add (0,0)!
			public void addToss() {
				addToss(0, false, handIndex);
			}

			//add (height, destHand)! or (sign(height)&, destHand)! (though destHand is irrelevant if it's infinite)
			public void addToss(int height, boolean isInfinite, int destHand) {
				addToss(height, isInfinite, destHand, false);
			}

			public void addToss(int height, boolean isInfinite, int destHand, boolean isAntiToss) {
				addToss(new Toss(handIndex, height, isInfinite, destHand, isAntiToss));
			}

			protected void addToss(Toss newToss) {
				//prevent redundant zero tosses
				if(isZeroHand()) {
					removeToss(0, true);
				}
				tosses.add(newToss);
				if(newToss.height != 0) {
					isEmpty = false;
				}
				hasInDegree = false;
			}

			protected void removeToss(int tossIndex) {
				removeToss(tossIndex, false);
			}

			protected void removeToss(int tossIndex, boolean dontAddZeroToss) {
				tosses.remove(tossIndex);
				if(!dontAddZeroToss && tosses.size() == 0) {
					addToss();
				}
				hasInDegree = false;
			}

			private Hand starHand(int newBeatIndex) {
				//flip hand index
				int newHandIndex = (handIndex + 1) % 2;
				Hand newHand = new Hand(newHandIndex, newBeatIndex);
				//add a copy of all tosses within this hand with altered startHand and destHand values
				for(Toss t : tosses) {
					newHand.addToss(t.starToss(newHandIndex));
				}
				return newHand;
			}

			public int numTosses() {
				return tosses.size();
			}

			private boolean isZeroHand() {
				if(tosses.size() > 0) {
					boolean allZero = true;
					for(Toss toss : tosses) {
						if(!toss.isZeroToss())
							allZero = false;
					}
					return allZero;
				} else {
					return false;
				}
			}

			public boolean isEmpty() {
				return isEmpty;
			}

			public Toss getToss(int index) {
				return tosses.get(index);
			}

			public Toss getLastToss() {
				return tosses.get(tosses.size() - 1);
			}

			private Hand deepCopy() {
				//get deep copy of each toss in tosses
				List<Toss> newTosses = new ArrayList<Toss>();
				for(int t=0; t<tosses.size(); t++) {
					newTosses.add(tosses.get(t).deepCopy());
				}
				return new Hand(newTosses, handIndex, beatIndex, isEmpty);
			}

			public String toString() {
				return tosses.toString();
			}

			protected class Toss {
				private int startHand;
				private int height;
				private boolean isInfinite;
				private int destHand;
				private boolean isAntiToss;

				public Toss(int startHand) {
					this(startHand, 0, false, startHand, false);
				}

				public Toss(int startHand, int height, int destHand) {
					this(startHand, height, false, destHand, false);
				}

				public Toss(int startHand, int height, boolean isInfinite, int destHand) {
					this(startHand, height, isInfinite, destHand, false);
				}

				private Toss(int startHand, int height, boolean isInfinite, int destHand, boolean isAntiToss) {
					this.startHand = startHand;
					this.height = height;
					this.isInfinite = isInfinite;
					this.isAntiToss = isAntiToss;
					this.destHand = destHand;
				}

				public int height() {
					return height;
				}

				public boolean isInfinite() {
					return isInfinite;
				}

				public boolean isAntiToss() {
					return isAntiToss;
				}

				protected void makeAntiToss(boolean isAntiToss) {
					if(this.isAntiToss != isAntiToss) {
						this.height = -this.height;
						this.isAntiToss = isAntiToss;
					}
				}

				public int startHand() {
					return startHand;
				}

				private boolean isZeroToss() {
					return (startHand == destHand && height == 0);
				}

				public int destHand() {
					return destHand;
				}

				public void setDestHand(int newDestHand) {
					this.destHand = newDestHand;
					hasInDegree = false;
				}

				public void flipDestHand() {
					this.destHand = (this.destHand + 1) % 2;
					hasInDegree = false;
				}

				private Toss starToss(int newHandIndex) {
					return new Toss(newHandIndex, height, isInfinite, (destHand + 1) % 2);
				}

				private Toss deepCopy() {
					return new Toss(startHand, height, isInfinite, destHand);
				}

				public String toString() {
					List<String> listToss = new ArrayList<String>();
					String heightString = "";
					if(isAntiToss) {
						heightString = "_";
					}
					if(!isInfinite) {
						heightString += ((Integer)height).toString();
					} else {
						if(height < 0) {
							//negative infinity
							heightString += "-&";
						} else if(height > 0) {
							//positive infinity
							heightString += "&";
						} else {
							//don't know how this would happen
							heightString += "0";
						}
					}
					listToss.add(heightString);
					listToss.add(((Integer)destHand).toString());
					return listToss.toString();
				}
			}
		}
	}

	public static void main(String[] args) {
		if(args.length == 1) {
			/*
			Siteswap ss = Parser.parse(args[0]);
			System.out.println(ss);
			ss.antiTossify();
			System.out.println("antiTossified:");
			System.out.println(ss);
			ss.unAntiTossify();
			System.out.println("unAntiTossified:");
			System.out.println(ss);
			*/

			Siteswap ss = new Siteswap(1, "async");
		}

	}

}


