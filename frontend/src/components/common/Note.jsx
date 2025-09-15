import React from 'react'

function Note(props) {
  return (
    <div>
        <h1>{`제목 : ${props.title}`}</h1>
        <h4>{`${props.content}`}</h4>
    </div>
  );
}

export default Note;