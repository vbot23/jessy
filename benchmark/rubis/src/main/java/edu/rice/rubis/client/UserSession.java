/*
 * RUBiS
 * Copyright (C) 2002, 2003, 2004 French National Institute For Research In Computer
 * Science And Control (INRIA).
 * Contact: jmob@objectweb.org
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or any later
 * version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * Initial developer(s): Emmanuel Cecchet, Julie Marguerite
 * Contributor(s): Jeremy Philippe, Massimo Neri <hello@mneri.me>
 */

package edu.rice.rubis.client;

import static fr.inria.jessy.transaction.TransactionState.*;

import fr.inria.jessy.Jessy;
import fr.inria.jessy.transaction.ExecutionHistory;
import fr.inria.jessy.transaction.Transaction;

import java.util.Date;
import java.util.Random;

import org.imdea.rubis.benchmark.transaction.*;
import org.imdea.rubis.benchmark.stats.StatsCollector;
import org.imdea.rubis.benchmark.util.TextUtils;

/**
 * RUBiS user session emulator. This class plays a random user session emulating
 * a Web browser.
 *
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet </a> and <a
 *         href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite </a>
 * @version 1.0
 */
public class UserSession extends Thread {
    private Jessy mJessy;
    private String mPassword;
    private Random mRand = new Random();
    private RUBiSProperties mProps;
    private StatsCollector mStatsCollector;
    private TransitionTable mTable;
    private int mUserId;
    private String mUsername;

    /**
     * Creates a new <code>UserSession</code> instance.
     *
     * @param threadId a thread identifier
     * @param rubis    rubis.properties
     */
    public UserSession(Jessy jessy, String threadId, RUBiSProperties rubis, StatsCollector collector) {
        super(threadId);
        mJessy = jessy;
        mProps = rubis;
        mTable = mProps.newTransitionTable();
        mStatsCollector = collector;
    }

    /**
     * Computes the URL to be accessed according to the given state. If any
     * parameter are needed, they are computed from last HTML reply.
     *
     * @param state current state
     * @return URL corresponding to the state
     */
    public Transaction nextTransaction(int state, int page, int nbOfItems) {
        Transaction trans = null;

        try {
            switch (state) {
                case -1:
                    mTable.resetToInitialState();
                    break;
                // Pages without transactions
                case 0:  // Home Page
                case 1:  // Register User Page
                case 3:  // Browse Page
                case 12: // Buy Now Auth Page
                case 21: // Sell Page
                case 22: // Select a category to sell item
                case 23: // Sell Item Form
                case 25: // About Me authentification
                    break;
                case 2: // Register the user in the database
                {
                    // Choose a random nb over already known attributed ids
                    int i = mProps.getNbOfUsers() + mRand.nextInt(1000000);
                    String firstname = "Great" + i;
                    String lastname = "User" + i;
                    String nickname = "user" + i;
                    String email = firstname + "." + lastname + "@rubis.com";
                    String password = "mPassword" + i;
                    String regionName = mProps.getRegions().get(i % mProps.getNbOfRegions());

                    trans = new RegisterUserTransaction(mJessy, i, firstname, lastname, nickname, password,
                            email, regionName);
                    break;
                }
                case 4: // Browse Categories
                    trans = new BrowseCategoriesTransaction(mJessy);
                    break;
                case 5: // Search item by category
                {
                    int categoryId = mRand.nextInt(mProps.getNbOfCategories());
                    trans = new SearchItemsByCategoryTransaction(mJessy, categoryId);
                    break;
                }
                case 6: // Browse Regions
                    trans = new BrowseCategoriesTransaction(mJessy);
                    break;
                case 7: // Browse categories in a region
                    String regionName = mProps.getRegions().get(mRand.nextInt(mProps.getNbOfRegions()));
                    trans = new BrowseCategoriesTransaction(mJessy, regionName);
                    break;
                case 8: // Browse items in a region for a given category
                {
                    int categoryId = mRand.nextInt(mProps.getNbOfCategories());
                    int regionId = mRand.nextInt(mProps.getNbOfRegions());

                    trans = new SearchItemsByRegionTransaction(mJessy, regionId, categoryId);
                    break;
                }
                case 9: // View an item
                {
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    trans = new ViewItemTransaction(mJessy, itemId);
                    break;
                }
                case 10: // View user information
                    trans = new ViewUserInfoTransaction(mJessy, mUserId);
                    break;
                case 11: // View item bid history
                {
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    trans = new ViewBidHistoryTransaction(mJessy, itemId);
                    break;
                }
                case 13: // Buy Now confirmation page
                {
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    trans = new BuyNowTransaction(mJessy, itemId, mUsername, mPassword);
                    break;
                }
                case 14: // Store Buy Now in the database
                {
                    int buyNowId = mRand.nextInt(1000000);
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    int maxQty = 100;
                    int qty = mRand.nextInt(maxQty) + 1;
                    trans = new StoreBuyNowTransaction(mJessy, buyNowId, mUserId, itemId, qty, new Date());
                    break;
                }
                case 15: // Put Bid Auth
                case 16: // Bid Authentication
                {
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    trans = new PutBidTransaction(mJessy, itemId, mUsername, mPassword);
                    break;
                }
                case 17: // Store Bid in the database
                {
                    int bidId = mProps.getNbOfBids() + mRand.nextInt(1000000);
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    int maxQty = 100;
                    int qty = mRand.nextInt(maxQty) + 1;
                    float minBid = 0.01f;
                    float addBid = mRand.nextInt(10) + 1;
                    float bid = minBid + addBid;
                    float maxBid = minBid + addBid * 2;
                    trans = new StoreBidTransaction(mJessy, bidId, mUserId, itemId, qty, bid, maxBid, new
                            Date());
                    break;
                }
                case 18: // Comment Authentication page
                case 19: // Comment confirmation page
                {
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    trans = new PutCommentTransaction(mJessy, mUserId, itemId, mUsername, mPassword);
                    break;
                }
                case 20: // Store Comment in the database
                {
                    int commentId = mProps.getNbOfComments() + mRand.nextInt(1000000);
                    int itemId = mRand.nextInt(mProps.getTotalItems());
                    int targetId = mRand.nextInt(mProps.getNbOfUsers());
                    int[] ratingValue = {-5, -3, 0, 3, 5};
                    int rating = mRand.nextInt(5);
                    int commentLength = mRand.nextInt(mProps.getCommentMaxLength()) + 1;
                    String comment = TextUtils.randomString(commentLength);

                    trans = new StoreCommentTransaction(mJessy, commentId, mUserId, targetId, itemId,
                            ratingValue[rating], new Date(), comment);
                    break;
                }
                case 24: // Store item in the database
                {
                    String name;
                    String description;
                    float initialPrice;
                    float reservePrice = 1000;
                    float buyNow = 1000;
                    int duration;
                    int quantity = 100;
                    int categoryId;
                    int totalItems = mProps.getTotalItems();
                    int i = totalItems + mRand.nextInt(1000000);

                    name = "RUBiS automatically generated item " + i;
                    int descriptionLength = mRand.nextInt(mProps.getItemDescriptionLength()) + 1;
                    description = TextUtils.randomString(descriptionLength);
                    initialPrice = mRand.nextInt(5000) + 1;
                    duration = mRand.nextInt(7) + 1;
                    Date startDate = new Date();
                    Date endDate = new Date(startDate.getTime() + duration * 1000 * 60 * 60 * 24);

                    categoryId = mRand.nextInt(mProps.getNbOfCategories());

                    trans = new RegisterItemTransaction(mJessy, i, name, description, initialPrice, quantity,
                            reservePrice, buyNow, 0, 0, startDate, endDate, mUserId, categoryId);
                    break;
                }
                case 26: // About Me information page
                    trans = new AboutMeTransaction(mJessy, mUserId, mUsername, mPassword);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trans;
    }

    /**
     * Emulate a user session using the current transition table.
     */
    public void run() {
        int lastTransition = -1;
        int nbOfItems = mProps.getNbOfItemsPerPage();
        int nextTransition;
        int page = 0;
        int transitionsLeft = mProps.getMaxNbOfTransitions();

        while (transitionsLeft > 0) {
            mUserId = mRand.nextInt(mProps.getNbOfUsers());
            mUsername = "user" + (mUserId);
            mPassword = "password" + (mUserId);
            mTable.resetToInitialState();
            nextTransition = mTable.getCurrentState();

            while (transitionsLeft > 0) {
                if (mTable.isEndOfSession())
                    mTable.resetToInitialState();

                Transaction trans = nextTransaction(nextTransition, page, nbOfItems);
                ExecutionHistory h = null;

                if (trans != null) {
                    try {
                        h = trans.execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        mStatsCollector.add(h);
                        lastTransition = nextTransition;

                        if (h != null && h.getTransactionState() == COMMITTED) {
                            mTable.nextState();
                        } else {
                            System.err.println("Failed to execute transaction " + trans.getClass().getSimpleName());
                            mTable.resetToInitialState();
                        }
                    }

                    transitionsLeft--;
                }

                nextTransition = mTable.nextState();
                page = lastTransition == nextTransition ? page + 1 : 0;
            }
        }
    }
}
