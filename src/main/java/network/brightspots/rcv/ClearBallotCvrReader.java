/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */

package network.brightspots.rcv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.util.Pair;
import network.brightspots.rcv.CastVoteRecord.CvrParseException;


class ClearBallotCvrReader {

  private final String cvrPath;
  private final ContestConfig contestConfig;

  ClearBallotCvrReader(String cvrPath, ContestConfig contestConfig) {
    this.cvrPath = cvrPath;
    this.contestConfig = contestConfig;
  }

  // parse Cvr json into CastVoteRecord objects and append them to the input castVoteRecords list
  // see Clear Ballot 2.1 RCV Format Specification for details
  void readCastVoteRecords(List<CastVoteRecord> castVoteRecords, String contestId)
      throws CvrParseException {
    BufferedReader csvReader;
    try {
      csvReader = new BufferedReader(new FileReader(this.cvrPath));
      // each "choice column" in the input Csv corresponds to a unique ranking: candidate+rank pair
      // we parse these rankings from the header row into a map for lookup during CVR parsing
      String firstRow = csvReader.readLine();
      if (firstRow == null) {
        Logger.log(Level.SEVERE, "No header row found in cast vote record file: %s", this.cvrPath);
        throw new CvrParseException();
      }
      String[] headerData = firstRow.split(",");
      if (headerData.length < CvrColumnField.ChoicesBegin.ordinal()) {
        Logger.log(Level.SEVERE, "No choice columns found in cast vote record file: %s",
            this.cvrPath);
        throw new CvrParseException();
      }
      Map<Integer, Pair<Integer, String>> columnIndexToRanking = new HashMap<>();
      for (int columnIndex = CvrColumnField.ChoicesBegin.ordinal(); columnIndex < headerData.length;
          columnIndex++) {
        String choiceColumnHeader = headerData[columnIndex];
        String[] choiceFields = choiceColumnHeader.split(":");
        // validate field count
        if (choiceFields.length != RcvChoiceHeaderField.FIELD_COUNT.ordinal()) {
          Logger.log(Level.SEVERE,
              "Wrong number of choice header fields in cast vote record file: %s",
              this.cvrPath);
          throw new CvrParseException();
        }
        // filter by contest
        String contestName = choiceFields[RcvChoiceHeaderField.CONTEST_NAME.ordinal()];
        if (!contestName.equals(contestId)) {
          continue;
        }
        // validate and store the ranking associated with this choice column
        String choiceName = choiceFields[RcvChoiceHeaderField.CHOICE_NAME.ordinal()];
        if (!this.contestConfig.getCandidateCodeList().contains(choiceName)) {
          Logger.log(Level.SEVERE, "Candidate: %s from cast vote record not found in config!",
              choiceName);
          throw new CvrParseException();
        }
        Integer rank = Integer.parseInt(choiceFields[RcvChoiceHeaderField.RANK.ordinal()]);
        if (rank > this.contestConfig.getMaxRankingsAllowed()) {
          Logger.log(Level.SEVERE, "Rank: %d exceeds max rankings allowed in config: %d", rank,
              this.contestConfig.getMaxRankingsAllowed());
          throw new CvrParseException();
        }
        columnIndexToRanking.put(columnIndex, new Pair<>(rank, choiceName));
      }
      // read all remaining rows and create CastVoteRecords for each one
      while (true) {
        String row = csvReader.readLine();
        if (row == null) {
          break;
        }
        // parse rankings
        String[] cvrData = row.split(",");
        ArrayList<Pair<Integer, String>> rankings = new ArrayList<>();
        for (int columnIndex : columnIndexToRanking.keySet()) {
          if (Integer.parseInt(cvrData[columnIndex]) == 1) {
            // user marked this column
            Pair<Integer, String> ranking = columnIndexToRanking.get(columnIndex);
            rankings.add(ranking);
          }
        }
        // create the cast vote record
        CastVoteRecord castVoteRecord = new CastVoteRecord(contestId,
            cvrData[CvrColumnField.ScanComputerName.ordinal()],
            null,
            cvrData[CvrColumnField.BallotID.ordinal()],
            cvrData[CvrColumnField.PrecinctID.ordinal()],
            null,
            cvrData[CvrColumnField.BallotStyleID.ordinal()],
            rankings
        );

        castVoteRecords.add(castVoteRecord);
        // provide some user feedback on the Cvr count
        if (castVoteRecords.size() % 50000 == 0) {
          Logger.log(Level.INFO, "Parsed %d cast vote records.", castVoteRecords.size());
        }
      }
      csvReader.close();
    } catch (FileNotFoundException e) {
      Logger.log(Level.SEVERE, "Cast vote record file not found!\n%s", e.toString());
    } catch (IOException e) {
      Logger.log(Level.SEVERE, "Error reading file!\n%s", e.toString());
    }
  }

  // These values correspond to the data in Clear Vote Cvr Csv columns
  public enum CvrColumnField {
    RowNumber,
    BoxID,
    BoxPosition,
    BallotID,
    PrecinctID,
    BallotStyleID,
    PrecinctStyleName,
    ScanComputerName,
    Status,
    Remade,
    ChoicesBegin
  }

  // These values correspond to the data in Clear Vote Rcv choice column header fields
  public enum RcvChoiceHeaderField {
    HEADER,
    CONTEST_NAME,
    RANK,
    VOTE_RULE,
    CHOICE_NAME,
    PARTY_NAME,
    FIELD_COUNT
  }
}