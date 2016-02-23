/*
 * RUBiS Benchmark
 * Copyright (C) 2016 IMDEA Software Institute
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
 */

package org.imdea.rubis.benchmark.transaction;

import fr.inria.jessy.Jessy;
import fr.inria.jessy.transaction.ExecutionHistory;

import java.util.Date;

import org.imdea.rubis.benchmark.entity.CommentEntity;
import org.imdea.rubis.benchmark.entity.UserEntity;

public class StoreCommentTransaction extends AbsRUBiSTransaction {
    private CommentEntity mComment;

    public StoreCommentTransaction(Jessy jessy, long id, String fromUserKey, String toUserKey, String itemKey, int
            rating, Date date, String comment) throws Exception {
        super(jessy);
        mComment = new CommentEntity(id, fromUserKey, toUserKey, itemKey, rating, date, comment);
    }

    @Override
    public ExecutionHistory execute() {
        try {
            // Insert the new comment in the data store.
            create(mComment);
            // Select the receiver from the data store and store the updated version of the user in the data store.
            UserEntity receiver = read(UserEntity.class, mComment.getToUserKey());
            receiver.edit().setRating(receiver.getRating() + mComment.getRating()).write(this);

            return commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
