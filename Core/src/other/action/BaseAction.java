package other.action;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import annotations.Hide;
import game.rules.play.moves.Moves;
import game.types.board.SiteType;
import game.util.directions.AbsoluteDirection;
import game.util.directions.DirectionFacing;
import game.util.graph.Radial;
import main.Constants;
import main.collections.FastArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import other.context.Context;
import other.move.Move;
import other.state.container.ContainerState;
import other.topology.Cell;
import other.topology.Topology;

/**
 * Action with default return values.
 * 
 * @author cambolbro and Eric.Piette
 */
@Hide
public abstract class BaseAction implements Action
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------
	
	/** decision action or not. */
	public boolean decision = false;

	//-------------------------------------------------------------------------
	
	@Override
	public int from()
	{
		return Constants.UNDEFINED;
	}
	
	@Override
	public SiteType fromType()
	{
		return SiteType.Cell;
	}

	@Override
	public int levelFrom()
	{
		return Constants.GROUND_LEVEL;
	}

	//-------------------------------------------------------------------------

	@Override
	public int to()
	{
		return Constants.UNDEFINED;
	}

	@Override
	public SiteType toType()
	{
		return SiteType.Cell;
	}

	@Override
	public int levelTo()
	{
		return Constants.GROUND_LEVEL;
	}

	//-------------------------------------------------------------------------

	@Override
	public int what()
	{
		return Constants.NO_PIECE;
	}

	@Override
	public int state()
	{
		return Constants.DEFAULT_STATE;
	}

	@Override
	public int rotation()
	{
		return Constants.DEFAULT_ROTATION;
	}

	@Override
	public int value()
	{
		return Constants.DEFAULT_ROTATION;
	}

	@Override
	public int count()
	{
		return Constants.NO_PIECE;
	}

	@Override
	public boolean isStacking()
	{
		return false;
	}

	@Override
	public boolean[] hidden()
	{
		return null;
	}

	@Override
	public int who()
	{
		return Constants.NOBODY;
	}

	@Override
	public boolean isDecision()
	{
		return decision;
	}

	@Override
	public boolean isPass()
	{
		return false;
	}
	
	@Override
	public boolean isForfeit()
	{
		return false;
	}

	@Override
	public boolean isSwap()
	{
		return false;
	}
	
	@Override
	public boolean isVote()
	{
		return false;
	}
	
	@Override
	public boolean isPropose()
	{
		return false;
	}
	
	@Override
	public boolean isAlwaysGUILegal()
	{
		return false;
	}

	@Override
	public String proposition()
	{
		return null;
	}

	@Override
	public String vote()
	{
		return null;
	}

	@Override
	public String message()
	{
		return null;
	}

	@Override
	public void setDecision(final boolean decision)
	{
		this.decision = decision;
	}

	@Override
	public Action withDecision(final boolean dec)
	{
		decision = dec;
		return this;
	}

	@Override
	public ActionType actionType() // TEMPORARY UNTIL ALL THE SUPER ACTIONS ARE NOT DONE.
	{
		return null;
	}

	@Override
	public boolean matchesUserMove(final int siteA, final int levelA, final SiteType graphElementTypeA, final int siteB,
			final int levelB, final SiteType graphElementTypeB)
	{
		return (from() == siteA && levelFrom() == levelA && fromType() == graphElementTypeA && to() == siteB
				&& levelTo() == levelB
						&& toType() == graphElementTypeB);
	}

	//-------------------------------------------------------------------------
	
	/**
	 * To update the playable site with the line of play of the dominoes.
	 * 
	 * @param context
	 * @param site
	 * @param dirn
	 */
	protected static void lineOfPlayDominoes(final Context context, final int site1, final int site2,
			final AbsoluteDirection dirn, final boolean doubleDomino, final boolean leftOrientation)
	{
		final ContainerState cs = context.containerState(0);
		final Topology topology = context.topology();
		final Cell c1 = context.topology().cells().get(site1);
		final Cell c2 = context.topology().cells().get(site2);

		final List<Radial> radialsC1 = topology.trajectories().radials(SiteType.Cell, c1.index(), dirn);
		final List<Radial> radialsC2 = topology.trajectories().radials(SiteType.Cell, c2.index(), dirn);

		if (radialsC1.size() > 0 && radialsC2.size() > 0)
		{
			final Radial radialC1 = radialsC1.get(0);
			final Radial radialC2 = radialsC2.get(0);
			
			if (radialC1.steps().length > 2 && radialC2.steps().length > 2
					&& cs.isEmpty(radialC1.steps()[1].id(), SiteType.Cell)
					&& cs.isEmpty(radialC2.steps()[1].id(), SiteType.Cell))
			{
				for (int i = 1; i < radialC1.steps().length && i < radialC2.steps().length
						&& i < 5; i++)
				{
					final int to = radialC1.steps()[i].id();
					final int to2 = radialC2.steps()[i].id();
					if (cs.isEmpty(to, SiteType.Cell) && cs.isEmpty(to2, SiteType.Cell))
					{
						cs.setPlayable(context.state(), to, true);
						cs.setPlayable(context.state(), to2, true);

						if (!doubleDomino && i < 3)
						{
							final DirectionFacing direction = AbsoluteDirection.convert(dirn);
							final DirectionFacing leftDirection = direction.left().left();
							final AbsoluteDirection absoluteLeftDirection = leftDirection.toAbsolute();
							final DirectionFacing rightDirection = direction.right().right();
							final AbsoluteDirection absoluteRightDirection = rightDirection.toAbsolute();

							if (leftOrientation)
							{
								final List<Radial> radialsC1Left = topology.trajectories().radials(SiteType.Cell,
										c1.index(), absoluteLeftDirection);
								final List<Radial> radialsC2Right = topology.trajectories().radials(SiteType.Cell,
										c2.index(), absoluteRightDirection);

								if (radialsC1Left.size() > 0 && radialsC1Left.get(0).steps().length > 1)
								{
									final int leftOfTo = radialsC1Left.get(0).steps()[1].id();
									if (cs.isEmpty(leftOfTo, SiteType.Cell))
										cs.setPlayable(context.state(), leftOfTo, true);
								}
								if (radialsC2Right.size() > 0 && radialsC2Right.get(0).steps().length > 1)
								{
									final int leftOfTo = radialsC2Right.get(0).steps()[1].id();
									if (cs.isEmpty(leftOfTo, SiteType.Cell))
										cs.setPlayable(context.state(), leftOfTo, true);
								}
							}
							else
							{
								final List<Radial> radialsC1Right = topology.trajectories().radials(SiteType.Cell,
										c1.index(), absoluteRightDirection);
								final List<Radial> radialsC2Left = topology.trajectories().radials(SiteType.Cell,
										c2.index(), absoluteLeftDirection);

								if (radialsC1Right.size() > 0 && radialsC1Right.get(0).steps().length > 1)
								{
									final int leftOfTo = radialsC1Right.get(0).steps()[1].id();
									if (cs.isEmpty(leftOfTo, SiteType.Cell))
										cs.setPlayable(context.state(), leftOfTo, true);
								}
								if (radialsC2Left.size() > 0 && radialsC2Left.get(0).steps().length > 1)
								{
									final int leftOfTo = radialsC2Left.get(0).steps()[1].id();
									if (cs.isEmpty(leftOfTo, SiteType.Cell))
										cs.setPlayable(context.state(), leftOfTo, true);
								}
							}
						}

					}
					else
						return;
				}
			}
		}
	}

	/**
	 * @param side
	 * @param state
	 * @return The direction of the line of play according to the side and the state
	 *         of the domino.
	 */
	@SuppressWarnings("static-method")
	protected AbsoluteDirection getDirnDomino(final int side, final int state)
	{
		switch (side)
		{
		case 0: // WEST SIDE
			switch (state)
			{
			case 0:
				return AbsoluteDirection.W;
			case 1:
				return AbsoluteDirection.N;
			case 2:
				return AbsoluteDirection.E;
			case 3:
				return AbsoluteDirection.S;
			}
			break;
		case 1: // NORTH SIDE
			switch (state)
			{
			case 0:
				return AbsoluteDirection.N;
			case 1:
				return AbsoluteDirection.E;
			case 2:
				return AbsoluteDirection.S;
			case 3:
				return AbsoluteDirection.W;
			}
			break;
		case 2: // EAST SIDE
			switch (state)
			{
			case 0:
				return AbsoluteDirection.E;
			case 1:
				return AbsoluteDirection.S;
			case 2:
				return AbsoluteDirection.W;
			case 3:
				return AbsoluteDirection.N;
			}
			break;
		case 3: // SOUTH SIDE
			switch (state)
			{
			case 0:
				return AbsoluteDirection.S;
			case 1:
				return AbsoluteDirection.W;
			case 2:
				return AbsoluteDirection.N;
			case 3:
				return AbsoluteDirection.E;
			}
			break;
		default:
			return null;
		}
		return null;
	}

	@Override
	public void setLevelFrom(final int levelA)
	{
		// do nothing in general.
	}

	@Override
	public void setLevelTo(final int levelB)
	{
		// do nothing in general.
	}

	@Override
	public boolean isOtherMove()
	{
		return false;
	}
	
	@Override
	public boolean isForced()
	{
		return false;
	}
	
	@Override
	public boolean containsNextInstance()
	{
		return false;
	}

	@Override
	public int playerSelected()
	{
		return Constants.UNDEFINED;
	}

	@Override
	public String toString()
	{
		return toTrialFormat(null);
	}

	@Override
	public String toMoveFormat(final Context context, final boolean useCoords)
	{
		return toTrialFormat(context);
	}

	@Override
	public BitSet concepts(final Context context, final Moves movesLudeme)
	{
		return new BitSet();
	}

    @Override
    public JSONObject toBourrytoFormat(){
        JSONObject j = new JSONObject();
        j.put("from", this.from());
        j.put("to", this.to());
        j.put("what", this.what());
        j.put("actionType", this.actionType().toString());

        // todo: not containing actions in moves so far

        return j;
    }

    public boolean same(String other, Context context){
        if (other == null || other.isEmpty()) return false;
        // from PlayerDesktop.app.menu.MainMenuFunctions under remote > select move from string:
        final FastArrayList<Move> substringMatchingMoves = new FastArrayList<>();
        other = other.toLowerCase().replace(" ", "").replace("\n", ""); // remove new lines and whitespaces

        List<String> formats = new LinkedList<>();
        formats.add(this.toTrialFormat(context));
        formats.add(this.toTurnFormat(context, true));
        formats.add(this.toTurnFormat(context, false));
        formats.add(this.toMoveFormat(context, true));
        formats.add(this.toMoveFormat(context, false));
        formats.add(this.toString());
        formats = formats.stream().map(string -> string.toLowerCase()
                                                                .replace(" ", "")
                                                                .replace("\n", "")).collect(Collectors.toList());
        for (String format : formats){
            if (format.equals(other) || format.contains(other)){
                return true;
            }
        }
        return false;
    }

	public JSONObject toJSON(){
		JSONObject jsonWrapper = new JSONObject();
		JSONObject json = new JSONObject();
		jsonWrapper.put("class", this.getClass().getName());
		jsonWrapper.put("package", this.getClass().getPackage());
		jsonWrapper.put("content", json);
		json.put("from", from());
		json.put("fromType", fromType());
		json.put("levelFrom", levelFrom());
		json.put("to", to());
		json.put("toType", toType());
		json.put("levelTo", levelTo());
		json.put("what", what());
		json.put("state", state());
		json.put("rotation", rotation());
		json.put("value", value());
		json.put("count", count());
		json.put("isStacking", isStacking());
		json.put("hidden", hidden());
		json.put("who", who());
		json.put("isDecision", isDecision());
		json.put("isPass", isPass());
		json.put("isForfeit", isForfeit());
		json.put("isSwap", isSwap());
		json.put("isVote", isVote());
		json.put("isPropose", isPropose());
		json.put("isAlwaysGUILegal", isAlwaysGUILegal());
		json.put("proposition", proposition());
		json.put("vote", vote());
		json.put("message", message());
		json.put("actionType", actionType());
		return jsonWrapper;
	}

    private String simplify(String string) {
        return string.toLowerCase().replace(" ", "").replace("\n", "");
    }
	// simple version of comparing incoming moves. Works for many moves and makes passing on parameters easier. used mostly for testing, not good stable verion for all cases
    @Override
    public boolean sameEnough(JSONObject other, boolean possibly_ignore_from) throws IllegalArgumentException{
        //if (other == null || other.isEmpty()) return false;
        // neccesary keys
        for (String key: Action.requiredComparisonKeys()){
//            if (key.equals("from") && this.from() == this.to()){continue;} // add move has same from and to, so we can accept only the to
            if (!other.has(key)){
                System.out.println("baseAction - sameEnought() - other doest have the key");
                throw new IllegalArgumentException(key + " is required but missing.");
            }
        }
        // from depending on game is important, i will leave it in as neccesary
        try {
			if (possibly_ignore_from && this.from() != this.to()) {
				if (other.getInt("from") != this.from()) {
					return false;
				}
			}
        } catch (JSONException e) {throw new IllegalArgumentException("'from' is required but missing.");}
        try {
            if (other.getInt("to") != this.to()){return false;}
        } catch (JSONException e) {throw new IllegalArgumentException("'to' is required but missing.");}
        try {
            if (other.getInt("what") != this.what()){return false;}
        } catch (JSONException e) {throw new IllegalArgumentException("'what' is required but missing.");}
        try {
            if (!other.getString("actionType").equalsIgnoreCase(this.actionType().toString())){return false;}
        } catch (JSONException ignored) {}
        try {
            if (!simplify(other.getString("action")).equals(simplify(this.actionType().toString()))){return false;}
        } catch (JSONException ignored) {}
        // these keys will be ignored if they don;t exist
        try {if (!simplify(other.getString("fromType")).equals(simplify(this.fromType().toString()))){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("levelFrom") != this.levelFrom()){return false;}} catch (JSONException ignored) {}
        try {if (!simplify(other.getString("toType")).equals(simplify(this.toType().toString()))){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("levelTo") != this.levelTo()){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("state") != this.state()){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("rotation") != this.rotation()){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("value") != this.value()){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("count") != this.count()){return false;}} catch (JSONException ignored) {}
        try {if (other.getInt("who") != this.who()){return false;}} catch (JSONException ignored) {}

        return true;
    }
}
