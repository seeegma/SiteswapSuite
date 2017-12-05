package siteswapsuite;

import java.util.List;
import java.util.ArrayList;

public class ArgTree {
	// global options
	// Set<String> debugClasses;
	List<ArgChain> argChains;

	public ArgTree() {
		this.argChains = new ArrayList<>();
	}

	private static List<ArgParseResult> parseArgsToFlatList(String[] args) throws ParseError {
		List<ArgParseResult> flatList = new ArrayList<>();
		for(int i=0; i<args.length; i++) {
			String arg = args[i];
			ArgParseResult parsedArg = ArgParseResult.parse(arg);
			// add follow-up if required and present
			Argument.FollowUp requires = parsedArg.head.arg.requires;
			if(requires == Argument.FollowUp.INT) {
				try {
					parsedArg.head.followUpInt = Integer.parseInt(args[++i]);
				} catch(NumberFormatException e) {
					throw new ParseError("follow-up '" + args[i-1] + "' cannot be coerced into an integer");
				} catch(ArrayIndexOutOfBoundsException e) {
					throw new ParseError("argument '" + args[i-1] + "' requires integer follow-up");
				}
			} else if(requires == Argument.FollowUp.STRING) {
				try {
					parsedArg.head.followUpString = args[++i];
				} catch(ArrayIndexOutOfBoundsException e) {
					throw new ParseError("argument '" + args[i-1] + "' requires string follow-up");
				}
			}
			flatList.add(parsedArg);
		}
		return flatList;
	}

	public static ArgTree parseArgTree(String[] args) throws ParseError {
		List<ArgParseResult> flatList = parseArgsToFlatList(args);
		ArgTree argTree = new ArgTree();
		for(ArgParseResult parsedArg : flatList) {
			switch(parsedArg.head.arg.ownRole) {
				case FIRST:
					if(argTree.argChains.size() == 0) {
						argTree.addGlobalArg(parsedArg);
					} else {
						throw new ParseError("argument '" + parsedArg.head.arg + "' must appear before all others");
					}
					break;
				case INPUT:
					argTree.addInputArg(parsedArg);
					break;
				case CHAIN:
					if(argTree.argChains.size() == 0) {
						throw new ParseError("argument '" + parsedArg.head.arg + "' must appear after an input argument");
					}
					if(parsedArg.head.arg == Argument.OPS) {
						// add a link for each operation to last chain
						for(ArgContainer operationArg : parsedArg.tail) {
							argTree.addOperationArg(operationArg);
						}
					} else { // Argument.INFO
						// add all info args to last link of last chain
						for(ArgContainer infoArg : parsedArg.tail) {
							argTree.addInfoArg(infoArg);
						}
					}
					break;
				case OPERATION:
					if(argTree.argChains.size() == 0) {
						throw new ParseError("argument '" + parsedArg.head.arg + "' must appear after an input argument");
					}
					argTree.addOperationArg(parsedArg.head);
					break;
				case INFO:
					if(argTree.argChains.size() == 0) {
						throw new ParseError("argument '" + parsedArg.head.arg + "' must appear after an input argument");
					}
					argTree.addInfoArg(parsedArg.head);
					break;
				default:
					throw new ParseError("argument '" + parsedArg.head.arg + "' appears in wrong place");
			}
		}
		return argTree;
	}

	private void addGlobalArg(ArgParseResult parsedArg) {
	}

	private void addInputArg(ArgParseResult parsedArg) {
		this.argChains.add(new ArgChain(parsedArg));
	}

	private void addOperationArg(ArgContainer operationArg) {
		this.getLastChain().newLink(operationArg);
	}

	private void addInfoArg(ArgContainer infoArg) {
		this.getLastChain().getLastLink().addInfoArg(infoArg);
	}

	private ArgChain getLastChain() {
		return this.argChains.get(this.argChains.size()-1);
	}

	class ArgChain {
		ArgContainer inputArg;
		List<ArgContainer> inputOptions;
		List<ArgLink> argLinks;

		ArgChain(ArgParseResult chainInput) {
			this.inputArg = chainInput.head;
			this.inputOptions = chainInput.tail;
			this.argLinks = new ArrayList<>();
			// add first link, with null operation
			// (infos to print about unmodified input go here)
			this.newLink(null);
		}

		void newLink(ArgContainer operation) {
			this.argLinks.add(new ArgLink(operation));
		}

		ArgLink getLastLink() {
			return this.argLinks.get(this.argLinks.size()-1);
		}

		class ArgLink {
			ArgContainer operation;
			List<ArgContainer> infoArgs;

			ArgLink(ArgContainer operation) {
				this.operation = operation;
				this.infoArgs = new ArrayList<>();
			}

			void addInfoArg(ArgContainer infoArg) {
				this.infoArgs.add(infoArg);
			}

			public String toString() {
				StringBuilder ret = new StringBuilder();
				if(this.operation != null) {
					ret.append(this.operation.toString());
				}
				ret.append(this.infoArgs.toString());
				return ret.toString();
			}
		}

		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append(this.inputArg.toString());
			ret.append(this.inputOptions.toString());
			ret.append(this.argLinks.toString());
			return ret.toString();
		}

	}

	public String toString() {
		return this.argChains.toString();
	}

	public static void main(String[] args) {
		try {
			ArgTree tree = ArgTree.parseArgTree(args);
			System.out.println(tree);
		} catch(ParseError e) {
			e.printStackTrace();
		}
	}

}