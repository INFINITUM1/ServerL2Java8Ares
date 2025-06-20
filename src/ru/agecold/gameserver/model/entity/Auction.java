/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.agecold.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import ru.agecold.Config;
import ru.agecold.L2DatabaseFactory;
import ru.agecold.gameserver.GameServer;
import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.datatables.ClanTable;
import ru.agecold.gameserver.idfactory.IdFactory;
import ru.agecold.gameserver.instancemanager.AuctionManager;
import ru.agecold.gameserver.instancemanager.ClanHallManager;
import ru.agecold.gameserver.model.L2Clan;
import ru.agecold.gameserver.model.L2World;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.mysql.Close;
import ru.agecold.mysql.Connect;
import ru.agecold.util.log.AbstractLogger;

public class Auction {

    protected static final Logger _log = AbstractLogger.getLogger(Auction.class.getName());
    private int _id = 0;
    private int _adenaId = Config.CLANHALL_PAYMENT;
    private long _endDate;
    private int _highestBidderId = 0;
    private String _highestBidderName = "";
    private int _highestBidderMaxBid = 0;
    private int _itemId = 0;
    private String _itemName = "";
    private int _itemObjectId = 0;
    private int _itemQuantity = 0;
    private String _itemType = "";
    private int _sellerId = 0;
    private String _sellerClanName = "";
    private String _sellerName = "";
    private int _currentBid = 0;
    private int _startingBid = 0;
    private Map<Integer, Bidder> _bidders = new FastMap<Integer, Bidder>();
    private static final String[] ItemTypeName = {
        "ClanHall"
    };

    public static enum ItemTypeEnum {

        ClanHall
    }

    public static class Bidder {

        private String _name;
        private String _clanName;
        private int _bid;
        private Calendar _timeBid;

        public Bidder(String name, String clanName, int bid, long timeBid) {
            _name = name;
            _clanName = clanName;
            _bid = bid;
            _timeBid = Calendar.getInstance();
            _timeBid.setTimeInMillis(timeBid);
        }

        public String getName() {
            return _name;
        }

        public String getClanName() {
            return _clanName;
        }

        public int getBid() {
            return _bid;
        }

        public Calendar getTimeBid() {
            return _timeBid;
        }

        public void setTimeBid(long timeBid) {
            _timeBid.setTimeInMillis(timeBid);
        }

        public void setBid(int bid) {
            _bid = bid;
        }
    }

    /** Task Sheduler for endAuction */
    public class AutoEndTask implements Runnable {

        public AutoEndTask() {
        }

        public void run() {
            try {
                endAuction();
            } catch (Throwable t) {
            }
        }
    }

    /** Constructor */
    public Auction(int auctionId) {
        _id = auctionId;
        load();
        startAutoTask();
    }

    public Auction(int itemId, L2Clan Clan, long delay, int bid, String name) {
        _id = itemId;
        _endDate = System.currentTimeMillis() + delay;
        _itemId = itemId;
        _itemName = name;
        _itemType = "ClanHall";
        _sellerId = Clan.getLeaderId();
        _sellerName = Clan.getLeaderName();
        _sellerClanName = Clan.getName();
        _startingBid = bid;
    }

    /** Load auctions */
    private void load() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            statement = con.prepareStatement("SELECT * FROM auction WHERE id = ?");
            statement.setInt(1, getId());
            rs = statement.executeQuery();

            while (rs.next()) {
                _currentBid = rs.getInt("currentBid");
                _endDate = rs.getLong("endDate");
                _itemId = rs.getInt("itemId");
                _itemName = rs.getString("itemName");
                _itemObjectId = rs.getInt("itemObjectId");
                _itemType = rs.getString("itemType");
                _sellerId = rs.getInt("sellerId");
                _sellerClanName = rs.getString("sellerClanName");
                _sellerName = rs.getString("sellerName");
                _startingBid = rs.getInt("startingBid");
            }
            loadBid();
        } catch (Exception e) {
            System.out.println("Exception: Auction.load(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
    }

    /** Load bidders **/
    private void loadBid() {
        Connect con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            con = L2DatabaseFactory.get();
            con.setTransactionIsolation(1);

            statement = con.prepareStatement("SELECT bidderId, bidderName, maxBid, clan_name, time_bid FROM auction_bid WHERE auctionId = ? ORDER BY maxBid DESC");
            statement.setInt(1, getId());
            rs = statement.executeQuery();

            while (rs.next()) {
                if (rs.isFirst()) {
                    _highestBidderId = rs.getInt("bidderId");
                    _highestBidderName = rs.getString("bidderName");
                    _highestBidderMaxBid = rs.getInt("maxBid");
                }
                _bidders.put(rs.getInt("bidderId"), new Bidder(rs.getString("bidderName"), rs.getString("clan_name"), rs.getInt("maxBid"), rs.getLong("time_bid")));
            }
        } catch (Exception e) {
            System.out.println("Exception: Auction.loadBid(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CSR(con, statement, rs);
        }
    }

    /** Task Manage */
    private void startAutoTask() {
        long currentTime = System.currentTimeMillis();
        long taskDelay = 0;
        if (_endDate <= currentTime) {
            _endDate = currentTime + 7 * 24 * 60 * 60 * 1000;
            saveAuctionDate();
        } else {
            taskDelay = _endDate - currentTime;
        }
        ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), taskDelay);
    }

    public static String getItemTypeName(ItemTypeEnum value) {
        return ItemTypeName[value.ordinal()];
    }

    /** Save Auction Data End */
    private void saveAuctionDate() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("Update auction set endDate = ? where id = ?");
            statement.setLong(1, _endDate);
            statement.setInt(2, _id);
            statement.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Exception: saveAuctionDate(): " + e.getMessage(), e);
        } finally {
            Close.CS(con, statement);
        }
    }

    /** Set a bid */
    public void setBid(L2PcInstance bidder, int bid) {
        int requiredAdena = bid;
        if (getHighestBidderName().equals(bidder.getClan().getLeaderName())) {
            requiredAdena = bid - getHighestBidderMaxBid();
        }
        if ((getHighestBidderId() > 0 && bid > getHighestBidderMaxBid())
                || (getHighestBidderId() == 0 && bid >= getStartingBid())) {
            if (takeItem(bidder, _adenaId, requiredAdena)) {
                updateInDB(bidder, bid);
                bidder.getClan().setAuctionBiddedAt(_id, true);
                return;
            }
        }
        bidder.sendMessage("Неправильная ставка!");
    }

    /** Return Item in WHC */
    private void returnItem(String cName, int itemId, int quantity, boolean penalty) {
        if (penalty) {
            quantity *= 0; //take 10% tax fee if needed
        }
        ClanTable.getInstance().getClanByName(cName).getWarehouse().addAdena(itemId, quantity);

        /*L2Clan clan = ClanTable.getInstance().getClanByName(cName);
        int adena = clan.getWarehouse().getAdena();
        Connect con = null;
        PreparedStatement statement = null;
        try
        {
        if (adena > 0)
        {
        con = L2DatabaseFactory.get();
        statement = con.prepareStatement("UPDATE items SET count=? WHERE owner_id =? AND item_id =?");
        statement.setInt(1, (quantity + adena));
        statement.setInt(2, clan.getClanId());
        statement.setInt(3, itemId);
        statement.execute();
        }
        else
        {
        statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,aug_id,aug_skill,aug_lvl,price_sell,price_buy,object_id,custom_type1,custom_type2,mana_left) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        statement.setInt(1, clan.getClanId());
        statement.setInt(2, itemId);
        statement.setInt(3, quantity);
        statement.setString(4, "WAREHOUSE");
        statement.setInt(5, 0);
        statement.setInt(6, 0);
        statement.setInt(7, -1);
        statement.setInt(8, -1);
        statement.setInt(9, -1);
        statement.setInt(10, 0);
        statement.setInt(11, 0);
        statement.setInt(12, IdFactory.getInstance().getNextId());
        statement.setInt(13, 0);
        statement.setInt(14, 0);
        statement.setInt(15, -1);
        statement.executeUpdate();
        }
        }
        catch (Exception e)
        {
        _log.log(Level.SEVERE, "Exception: Auction.updateInDB(L2PcInstance bidder, int bid): " + e.getMessage());
        e.printStackTrace();
        }
        finally
        {
        Close.CS(con, statement);
        }*/
    }

    /** Take Item in WHC */
    private boolean takeItem(L2PcInstance bidder, int itemId, int quantity) {
        if (bidder.getClan() != null && bidder.getClan().getWarehouse().getItemCount(itemId) >= quantity) {
            bidder.getClan().getWarehouse().reduceAdena(itemId, quantity, bidder, bidder);
            return true;
        }
        bidder.sendMessage("Не хватает адены");
        return false;
    }

    /** Update auction in DB */
    private void updateInDB(L2PcInstance bidder, int bid) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            if (getBidders().get(bidder.getClanId()) != null) {
                statement = con.prepareStatement("UPDATE auction_bid SET bidderId=?, bidderName=?, maxBid=?, time_bid=? WHERE auctionId=? AND bidderId=?");
                statement.setInt(1, bidder.getClanId());
                statement.setString(2, bidder.getClan().getLeaderName());
                statement.setInt(3, bid);
                statement.setLong(4, System.currentTimeMillis());
                statement.setInt(5, getId());
                statement.setInt(6, bidder.getClanId());
                statement.execute();
                Close.S(statement);
            } else {
                statement = con.prepareStatement("INSERT INTO auction_bid (id, auctionId, bidderId, bidderName, maxBid, clan_name, time_bid) VALUES (?, ?, ?, ?, ?, ?, ?)");
                statement.setInt(1, IdFactory.getInstance().getNextId());
                statement.setInt(2, getId());
                statement.setInt(3, bidder.getClanId());
                statement.setString(4, bidder.getName());
                statement.setInt(5, bid);
                statement.setString(6, bidder.getClan().getName());
                statement.setLong(7, System.currentTimeMillis());
                statement.execute();
                Close.S(statement);
                if (L2World.getInstance().getPlayer(_highestBidderName) != null) {
                    L2World.getInstance().getPlayer(_highestBidderName).sendMessage("You have been out bidded");
                }
            }
            _highestBidderId = bidder.getClanId();
            _highestBidderMaxBid = bid;
            _highestBidderName = bidder.getClan().getLeaderName();
            if (_bidders.get(_highestBidderId) == null) {
                _bidders.put(_highestBidderId, new Bidder(_highestBidderName, bidder.getClan().getName(), bid, Calendar.getInstance().getTimeInMillis()));
            } else {
                _bidders.get(_highestBidderId).setBid(bid);
                _bidders.get(_highestBidderId).setTimeBid(Calendar.getInstance().getTimeInMillis());
            }
            bidder.sendMessage("Ставка сделана");
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Exception: Auction.updateInDB(L2PcInstance bidder, int bid): " + e.getMessage());
            e.printStackTrace();
        } finally {
            Close.CS(con, statement);
        }
    }

    /** Remove bids */
    private void removeBids() {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=?");
            statement.setInt(1, getId());
            statement.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
        } finally {
            Close.CS(con, statement);
        }

        for (Bidder b : _bidders.values()) {
            if (ClanTable.getInstance().getClanByName(b.getClanName()).getHasHideout() == 0) {
                returnItem(b.getClanName(), _adenaId, 9 * b.getBid() / 10, false); // 10 % tax
            } else {
                if (L2World.getInstance().getPlayer(b.getName()) != null) {
                    L2World.getInstance().getPlayer(b.getName()).sendMessage("Congratulation you have won ClanHall!");
                }
            }
            ClanTable.getInstance().getClanByName(b.getClanName()).setAuctionBiddedAt(0, true);
        }
        _bidders.clear();
    }

    /** Remove auctions */
    public void deleteAuctionFromDB() {
        AuctionManager.getInstance().getAuctions().remove(this);
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();
            statement = con.prepareStatement("DELETE FROM auction WHERE itemId=?");
            statement.setInt(1, _itemId);
            statement.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Exception: Auction.deleteFromDB(): " + e.getMessage(), e);
        } finally {
            Close.CS(con, statement);
        }
    }

    /** End of auction */
    public void endAuction() {
        if (GameServer.gameServer.getCHManager() != null && GameServer.gameServer.getCHManager().loaded()) {
            if (_highestBidderId == 0 && _sellerId == 0) {
                startAutoTask();
                return;
            }
            if (_highestBidderId == 0 && _sellerId > 0) {
                /** If seller haven't sell ClanHall, auction removed,
                 *  THIS MUST BE CONFIRMED */
                int aucId = AuctionManager.getInstance().getAuctionIndex(_id);
                AuctionManager.getInstance().getAuctions().remove(aucId);
                return;
            }
            if (_sellerId > 0) {
                returnItem(_sellerClanName, _adenaId, _highestBidderMaxBid, true);
                returnItem(_sellerClanName, _adenaId, ClanHallManager.getInstance().getClanHallById(_itemId).getLease(), false);
            }
            deleteAuctionFromDB();
            L2Clan Clan = ClanTable.getInstance().getClanByName(_bidders.get(_highestBidderId).getClanName());
            _bidders.remove(_highestBidderId);
            Clan.setAuctionBiddedAt(0, true);
            removeBids();
            ClanHallManager.getInstance().setOwner(_itemId, Clan);
        } else {
            /** Task waiting ClanHallManager is loaded every 3s */
            ThreadPoolManager.getInstance().scheduleGeneral(new AutoEndTask(), 3000);
        }
    }

    /** Cancel bid */
    public void cancelBid(int bidder) {
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement("DELETE FROM auction_bid WHERE auctionId=? AND bidderId=?");
            statement.setInt(1, getId());
            statement.setInt(2, bidder);
            statement.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Exception: Auction.cancelBid(String bidder): " + e.getMessage(), e);
        } finally {
            Close.CS(con, statement);
        }

        returnItem(_bidders.get(bidder).getClanName(), _adenaId, _bidders.get(bidder).getBid(), true);
        ClanTable.getInstance().getClanByName(_bidders.get(bidder).getClanName()).setAuctionBiddedAt(0, true);
        _bidders.clear();
        loadBid();
    }

    /** Cancel auction */
    public void cancelAuction() {
        deleteAuctionFromDB();
        removeBids();
    }

    /** Confirm an auction */
    public void confirmAuction() {
        AuctionManager.getInstance().getAuctions().add(this);
        Connect con = null;
        PreparedStatement statement = null;
        try {
            con = L2DatabaseFactory.get();

            statement = con.prepareStatement("INSERT INTO auction (id, sellerId, sellerName, sellerClanName, itemType, itemId, itemObjectId, itemName, itemQuantity, startingBid, currentBid, endDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            statement.setInt(1, getId());
            statement.setInt(2, _sellerId);
            statement.setString(3, _sellerName);
            statement.setString(4, _sellerClanName);
            statement.setString(5, _itemType);
            statement.setInt(6, _itemId);
            statement.setInt(7, _itemObjectId);
            statement.setString(8, _itemName);
            statement.setInt(9, _itemQuantity);
            statement.setInt(10, _startingBid);
            statement.setInt(11, _currentBid);
            statement.setLong(12, _endDate);
            statement.execute();

            loadBid();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Exception: Auction.load(): " + e.getMessage(), e);
        } finally {
            Close.CS(con, statement);
        }
    }

    /** Get var auction */
    public final int getId() {
        return _id;
    }

    public final int getCurrentBid() {
        return _currentBid;
    }

    public final long getEndDate() {
        return _endDate;
    }

    public final int getHighestBidderId() {
        return _highestBidderId;
    }

    public final String getHighestBidderName() {
        return _highestBidderName;
    }

    public final int getHighestBidderMaxBid() {
        return _highestBidderMaxBid;
    }

    public final int getItemId() {
        return _itemId;
    }

    public final String getItemName() {
        return _itemName;
    }

    public final int getItemObjectId() {
        return _itemObjectId;
    }

    public final int getItemQuantity() {
        return _itemQuantity;
    }

    public final String getItemType() {
        return _itemType;
    }

    public final int getSellerId() {
        return _sellerId;
    }

    public final String getSellerName() {
        return _sellerName;
    }

    public final String getSellerClanName() {
        return _sellerClanName;
    }

    public final int getStartingBid() {
        return _startingBid;
    }

    public final Map<Integer, Bidder> getBidders() {
        return _bidders;
    }
}