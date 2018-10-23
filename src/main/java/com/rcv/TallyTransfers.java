package com.rcv;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// TallyTransfers class stores summary info on vote transfers
// used primary as visualizer input to help build Sankey plots
class TallyTransfers {

  // Map of round number to vote transfers which occurred in that round
  // transfers for a round are a map of SOURCE candidate(s) to one or more TARGET candidates.
  // For each target candidate the map value is total vote values received from that source.
  // For round 1 source candidate is marked "uncounted" since the votes had no prior recipient.
  private final Map<Integer, Map<String, Map<String, BigDecimal>>> tallyTransfers = new HashMap<>();


  Map<String, Map<String, BigDecimal>> getTransfersForRound(Integer round) {
    return tallyTransfers.get(round);
  }

  // add vote transfer value for given round
  void addTransfer(Integer round,
      String sourceCandidate,
      String targetCandidate,
      BigDecimal value) {
    // null source means we are transferring the initial count
    if(sourceCandidate == null) {
      sourceCandidate = "uncounted";
    }
    // null target means exhausted
    if(targetCandidate == null) {
      targetCandidate = "exhausted";
    }

    // lookup or create transfer entries for specified round
    Map<String, Map<String, BigDecimal>> roundEntries = tallyTransfers
        .computeIfAbsent(round, k -> new HashMap<>());
    // lookup or create map for the source candidate
    Map<String, BigDecimal> candidateEntries = roundEntries
        .computeIfAbsent(sourceCandidate, k -> new HashMap<>());
    // lookup or create entry for the destination candidate
    BigDecimal currentValue = candidateEntries.getOrDefault(targetCandidate, BigDecimal.ZERO);
    // add transfer value and store the result
    BigDecimal newTally = currentValue.add(value);
    candidateEntries.put(targetCandidate, newTally);
  }

}