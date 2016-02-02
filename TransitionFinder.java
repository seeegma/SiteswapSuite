package siteswapsuite;

import java.util.List;

public class TransitionFinder {

	public static void printf(Object input) { System.out.println(input); }

	/*
	   IDEAS

	   -have commandline arguments for forcing it to parse a string as a ss w/ a particular number of hands
	   -and for starting with a particular hand

	 */

	private static void analyze(NotatedSiteswap ss) {
		printf("parsed: " + ss.toString());
		printf("de-parsed: " + ss.print());
		printf("number of balls: " + ss.numBalls());
		printf("number of hands: " + ss.numHands());
		printf("valid: " + ss.isValid());
		printf("period: " + ss.period());
		printf("state: " + new State(ss));
	}

	public static void main(String[] args) {
		if(args.length == 1) {
			try {
				NotatedSiteswap ss = NotatedSiteswap.parseSingle(args[0]);
				analyze(ss);
			} catch(InvalidNotationException e) {
				printf("invalid notation");
			}
		} else if(args.length == 2) {
			try {
				CompatibleNotatedSiteswapPair patterns = new CompatibleNotatedSiteswapPair(args[0], args[1]);
				analyze(patterns.prefix());
				printf("-----");
				analyze(patterns.suffix());
				printf("-----");
				try {
					ContextualizedNotatedTransitionList transitions = new ContextualizedNotatedTransitionList(patterns, 0, false, false);
					printf("General Transition:");
					printf(transitions.printGeneralTransition());
					printf(transitions.generalTransition());
					printf("All Transitions:");
					List<NotatedSiteswap> ts = transitions.transitionList();
					for(int i=0; i<ts.size(); i++) {
						printf(ts.get(i).print());
					}
				} catch(ImpossibleTransitionException e) {
					printf("ERROR: cannot compute transition between non-finite states");
				}
			} catch(InvalidNotationException e) {
				printf(e.getMessage());
			} catch(IncompatibleNotationException e) {
				printf(e.getMessage());
			}
		}
	}

}
