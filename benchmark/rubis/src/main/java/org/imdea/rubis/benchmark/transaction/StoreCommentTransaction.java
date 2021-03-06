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

    public StoreCommentTransaction(Jessy jessy, long id, long fromUserId, long toUserId, long itemId, int
            rating, Date date, String comment) throws Exception {
        super(jessy);
        mComment = new CommentEntity(id, fromUserId, toUserId, itemId, rating, date, comment);
    }

    @Override
    public ExecutionHistory execute() {
        try {
            create(mComment);
            UserEntity receiver = read(UserEntity.class, UserEntity.getKeyFromId(mComment.getToUserId()));
            receiver.edit().setRating(receiver.getRating() + mComment.getRating()).write(this);
            updateIndexes();

            return commitTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void updateIndexes() {
        create(new CommentEntity.FromUserIdIndex(mComment.getFromUserId(), mComment.getId()));
        create(new CommentEntity.ItemIdIndex(mComment.getItemId(), mComment.getId()));
        create(new CommentEntity.ToUserIdIndex(mComment.getToUserId(), mComment.getId()));
    }
}
